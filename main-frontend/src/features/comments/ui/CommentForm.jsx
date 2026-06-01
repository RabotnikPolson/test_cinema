import React, { useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '@/features/auth';
import './CommentForm.css';

export default function CommentForm({
  value,
  onChange,
  onSubmit,
  onCancel,
  placeholder,
  autoFocus = false
}) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const textareaRef = useRef(null);
  const inputPlaceholder = placeholder || t("comments.writePlaceholder");

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = textareaRef.current.scrollHeight + "px";
    }
  }, [value]);

  useEffect(() => {
    if (autoFocus && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [autoFocus]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && e.ctrlKey) {
      e.preventDefault();
      if (value.trim()) {
        onSubmit();
      }
    }
  };

  const hasText = value.trim().length > 0;

  return (
    <div className="comment-form-container">
      <div className="comment-form-avatar">
        {user?.avatarUrl ? (
          <img src={user.avatarUrl} alt={user.username} />
        ) : (
          <div className="comment-form-avatar-fallback">
            {user?.username?.[0]?.toUpperCase() || "U"}
          </div>
        )}
      </div>
      
      <div className="comment-form-content">
        <textarea
          ref={textareaRef}
          className="comment-form-input"
          placeholder={inputPlaceholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          rows={1}
        />

        <div className="comment-form-actions">
          {onCancel && (
            <button className="comment-form-btn-cancel" onClick={onCancel}>
              {t("common.cancel")}
            </button>
          )}
          <button
            className="comment-form-btn-submit"
            onClick={onSubmit}
            disabled={!hasText}
          >
            {t("common.send")}
          </button>
        </div>
      </div>
    </div>
  );
}
