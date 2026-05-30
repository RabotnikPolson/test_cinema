import React, { useRef, useEffect } from 'react';
import { useAuth } from '@/features/auth';
import './CommentForm.css';

export default function CommentForm({
  value,
  onChange,
  onSubmit,
  onCancel,
  placeholder = "Написать комментарий...",
  autoFocus = false
}) {
  const { user } = useAuth();
  const textareaRef = useRef(null);

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
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          rows={1}
        />
        
        <div className="comment-form-actions">
          {onCancel && (
            <button className="comment-form-btn-cancel" onClick={onCancel}>
              Отмена
            </button>
          )}
          <button 
            className="comment-form-btn-submit" 
            onClick={onSubmit}
            disabled={!hasText}
          >
            Отправить
          </button>
        </div>
      </div>
    </div>
  );
}
