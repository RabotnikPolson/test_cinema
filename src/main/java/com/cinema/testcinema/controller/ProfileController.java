package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.profile.*;
import com.cinema.testcinema.model.Rating;
import com.cinema.testcinema.model.Review;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.UserProfile;
import com.cinema.testcinema.repository.RatingRepository;
import com.cinema.testcinema.repository.ReviewRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import com.cinema.testcinema.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserProfileService userProfileService;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final ReviewRepository reviewRepository;

    public ProfileController(UserProfileService userProfileService,
                             AuthenticatedUserService authenticatedUserService,
                             UserRepository userRepository,
                             RatingRepository ratingRepository,
                             ReviewRepository reviewRepository) {
        this.userProfileService = userProfileService;
        this.authenticatedUserService = authenticatedUserService;
        this.userRepository = userRepository;
        this.ratingRepository = ratingRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/me")
    public ProfileMeDto getMe(Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        return userProfileService.getOwnProfile(userId);
    }

    @PutMapping("/me")
    public ProfileMeDto updateMe(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        return userProfileService.updateProfile(userId, request);
    }

    @GetMapping("/{username}")
    public Object getProfile(@PathVariable String username, Authentication authentication) {
        UserProfile profile = userProfileService.getProfileByUsername(username);
        User user = profile.getUser();
        boolean isOwner = isOwner(authentication, user);
        boolean isAdmin = authenticatedUserService.hasRole(authentication, "ADMIN");

        if (isOwner || isAdmin) {
            return userProfileService.toOwnerDto(user, profile);
        }
        return userProfileService.toPublicDto(user, profile);
    }

    @GetMapping("/{username}/ratings")
    public PublicProfileWithRatingsDto getRatings(@PathVariable String username,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  Authentication authentication) {
        UserProfile profile = userProfileService.getProfileByUsername(username);
        User user = profile.getUser();
        boolean isOwner = isOwner(authentication, user);
        boolean isAdmin = authenticatedUserService.hasRole(authentication, "ADMIN");

        if (profile.isPrivate() && !isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Профиль закрыт");
        }

        Page<Rating> ratingPage = ratingRepository.findByUserId(user.getId(), PageRequest.of(page, size));
        List<Long> movieIds = ratingPage.stream()
                .map(rating -> rating.getMovie().getId())
                .toList();
        Map<Long, Review> reviewsByMovie = movieIds.isEmpty() ? Map.of() : reviewRepository
                .findByUserIdAndMovieIdIn(user.getId(), movieIds)
                .stream()
                .collect(Collectors.toMap(Review::getMovieId, r -> r, (a, b) -> a));

        List<ProfileRatingDto> ratingDtos = ratingPage.getContent().stream()
                .map(rating -> {
                    Review review = reviewsByMovie.get(rating.getMovie().getId());
                    return new ProfileRatingDto(
                            rating.getMovie().getId(),
                            rating.getMovie().getTitle(),
                            rating.getMovie().getPosterUrl(),
                            rating.getMovie().getImdbId(),
                            rating.getScore(),
                            review != null ? review.getContent() : null
                    );
                })
                .toList();

        PublicProfileDto profileDto = (isOwner || isAdmin)
                ? new PublicProfileDto(user.getUsername(), profile.getAvatarUrl(), user.getCreatedAt(), profile.getBio())
                : userProfileService.toPublicDto(user, profile);
        return new PublicProfileWithRatingsDto(
                profileDto,
                ratingDtos,
                ratingPage.getNumber(),
                ratingPage.getSize(),
                ratingPage.getTotalElements(),
                ratingPage.getTotalPages()
        );
    }

    private boolean isOwner(Authentication authentication, User user) {
        Long currentId = authenticatedUserService.getCurrentUserIdIfAuthenticated(authentication);
        return Objects.equals(currentId, user.getId());
    }
}
