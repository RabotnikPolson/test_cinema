package com.cinema.testcinema.service;

import com.cinema.testcinema.exception.BusinessException;
import com.cinema.testcinema.model.EmailVerificationToken;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.UserProfile;
import com.cinema.testcinema.repository.EmailVerificationTokenRepository;
import com.cinema.testcinema.repository.UserProfileRepository;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EmailVerificationService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration EDIT_WINDOW = Duration.ofDays(7);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserProfileRepository userProfileRepository,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void requestEmailChange(User user, String newEmail) {
        UserProfile profile = ensureProfile(user);

        if (!canEditNicknameOrEmail(profile)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Изменять никнейм или email можно раз в 7 дней");
        }

        if (userRepository.existsByEmail(newEmail) && !newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email уже зарегистрирован");
        }

        List<EmailVerificationToken> activeTokens = tokenRepository.findByUserIdAndConsumedFalse(user.getId());
        activeTokens.forEach(token -> token.setConsumed(true));
        tokenRepository.saveAll(activeTokens);

        String code = generateCode();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setNewEmail(newEmail);
        token.setCodeHash(passwordEncoder.encode(code));
        token.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        tokenRepository.save(token);

        profile.setEmail(newEmail);
        profile.setEmailVerified(false);
        userProfileRepository.save(profile);

        emailService.sendEmailVerificationCode(newEmail, code);
    }

    @Transactional
    public void verifyCode(User user, String code) {
        EmailVerificationToken token = tokenRepository
                .findTopByUserIdAndConsumedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Нет активного кода подтверждения"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setConsumed(true);
            tokenRepository.save(token);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Код подтверждения истёк");
        }

        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Неверный код подтверждения");
        }

        UserProfile profile = ensureProfile(user);

        if (!canEditNicknameOrEmail(profile)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Изменять никнейм или email можно раз в 7 дней");
        }

        profile.setEmail(token.getNewEmail());
        profile.setEmailVerified(true);
        profile.setLastProfileEditAt(Instant.now());
        userProfileRepository.save(profile);

        user.setEmail(token.getNewEmail());
        userRepository.save(user);

        token.setConsumed(true);
        tokenRepository.save(token);
    }

    private String generateCode() {
        int number = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", number);
    }

    private boolean canEditNicknameOrEmail(UserProfile profile) {
        Instant lastEdit = profile.getLastProfileEditAt();
        if (lastEdit == null) {
            return true;
        }
        return !lastEdit.plus(EDIT_WINDOW).isAfter(Instant.now());
    }

    private UserProfile ensureProfile(User user) {
        return userProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setEmail(user.getEmail());
            return userProfileRepository.save(profile);
        });
    }
}
