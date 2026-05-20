export default function ReviewReadModal({ open, review, onClose }) {
    if (!open || !review) return null;

    const date = review.createdAt ? new Date(review.createdAt).toLocaleString() : "";

    return (
        <div
            style={{
                position: "fixed",
                inset: 0,
                background: "rgba(0,0,0,0.6)",
                zIndex: 9999,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                padding: 16,
            }}
            onClick={onClose}
        >
            <div
                style={{
                    width: "min(900px, 96vw)",
                    background: "#1c1c1f",
                    borderRadius: 16,
                    padding: 24,
                    color: "#fff",
                    position: "relative",
                }}
                onClick={(e) => e.stopPropagation()}
            >
                <button
                    onClick={onClose}
                    style={{
                        position: "absolute",
                        top: 12,
                        right: 12,
                        width: 36,
                        height: 36,
                        borderRadius: 999,
                        border: "none",
                        background: "rgba(255,255,255,0.08)",
                        color: "#fff",
                        cursor: "pointer",
                        fontSize: 18,
                    }}
                    aria-label="Close"
                >
                    ×
                </button>

                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <div
                        style={{
                            width: 44,
                            height: 44,
                            borderRadius: 12,
                            background: "rgba(255,255,255,0.08)",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            overflow: "hidden",
                            flex: "0 0 auto",
                        }}
                    >
                        {review.authorAvatarUrl ? (
                            <img
                                src={review.authorAvatarUrl}
                                alt=""
                                style={{ width: "100%", height: "100%", objectFit: "cover" }}
                            />
                        ) : (
                            <span style={{ opacity: 0.9 }}>👤</span>
                        )}
                    </div>

                    <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 700, fontSize: 18, lineHeight: 1.2 }}>
                            {review.authorUsername || "Пользователь"}
                            {typeof review.score === "number" && (
                                <span
                                    style={{
                                        marginLeft: 10,
                                        display: "inline-flex",
                                        alignItems: "center",
                                        justifyContent: "center",
                                        width: 38,
                                        height: 38,
                                        borderRadius: 10,
                                        background: "rgba(0, 200, 120, 0.25)",
                                        color: "#bfffe0",
                                        fontWeight: 800,
                                    }}
                                >
                  {review.score}
                </span>
                            )}
                        </div>
                        <div style={{ opacity: 0.7, marginTop: 4 }}>{date}</div>
                    </div>
                </div>

                <div style={{ marginTop: 18, fontSize: 18, lineHeight: 1.55, whiteSpace: "pre-wrap" }}>
                    {review.content || ""}
                </div>
            </div>
        </div>
    );
}
