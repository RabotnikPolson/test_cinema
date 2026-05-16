import { useState } from "react";

export default function CommentItem({
                                        node,
                                        onReply,
                                        onDelete,
                                        onReact,
                                        isReply = false,
                                    }) {
    const [replyText, setReplyText] = useState("");
    const [replying, setReplying] = useState(false);

    return (
        <div className={`comment ${isReply ? "reply" : ""}`}>
            <div className="comment-avatar">
                {node.authorAvatarUrl ? (
                    <img src={node.authorAvatarUrl} alt="" />
                ) : (
                    <div className="avatar-fallback">
                        {node.authorUsername?.[0]?.toUpperCase() ?? "?"}
                    </div>
                )}
            </div>

            <div className="comment-body">
                <div className="comment-meta">
                    <strong>{node.authorUsername ?? "Удалённый пользователь"}</strong>
                    <span>{new Date(node.createdAt).toLocaleString()}</span>
                </div>

                <div className="comment-text">{node.content}</div>

                <div className="comment-actions">
                    <button onClick={() => onReact("👍")}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path></svg>
                        {node.totalReactions || "Like"}
                    </button>

                    {!isReply && onReply && (
                        <button onClick={() => setReplying(!replying)}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 10 20 15 15 20"></polyline><path d="M4 4v7a4 4 0 0 0 4 4h12"></path></svg>
                            Ответить
                        </button>
                    )}

                    <button onClick={onDelete}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                        Удалить
                    </button>
                </div>

                {replying && (
                    <div className="reply-form">
            <textarea
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                placeholder="Ответить…"
            />
                        <button
                            onClick={() => {
                                onReply(replyText);
                                setReplyText("");
                                setReplying(false);
                            }}
                        >
                            Отправить
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
