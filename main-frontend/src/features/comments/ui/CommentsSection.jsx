import { useEffect, useMemo, useState } from "react";
import CommentItem from "./CommentItem";
import {
  useRootComments,
  useReplies,
  useCommentCount,
  useCommentMutations,
} from "@/features/comments/model/useComments";
import "@/shared/styles/components/comments.css";

function SortDropdown({ value, onChange }) {
  const [open, setOpen] = useState(false);

  const items = useMemo(
    () => [
      { value: "top", label: "Популярные" },
      { value: "new", label: "Сначала новые" },
      { value: "old", label: "Сначала старые" },
    ],
    []
  );

  const currentLabel = useMemo(() => {
    const found = items.find((i) => i.value === value);
    return found ? found.label : items[0].label;
  }, [items, value]);

  return (
    <div className="cselect" tabIndex={0} onBlur={() => setOpen(false)}>
      <button
        type="button"
        className="cselect__btn"
        onClick={() => setOpen((v) => !v)}
      >
        {currentLabel}
        <span className="cselect__chev">▾</span>
      </button>

      {open && (
        <div className="cselect__menu">
          {items.map((i) => (
            <button
              key={i.value}
              type="button"
              className={`cselect__item ${i.value === value ? "is-active" : ""}`}
              onMouseDown={(e) => {
                e.preventDefault();
                onChange(i.value);
                setOpen(false);
              }}
            >
              {i.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function CommentsSection({ movieId }) {
  const [order, setOrder] = useState("top");
  const [text, setText] = useState("");

  useEffect(() => {
    setOrder("top");
  }, [movieId]);

  const { data: count } = useCommentCount(movieId);
  const { data, isLoading } = useRootComments({ movieId, order });
  const mutations = useCommentMutations(movieId);

  const comments = data?.content ?? [];

  const submitRoot = () => {
    if (!text.trim()) return;

    mutations.create.mutate({
      movieId,
      content: text,
      parentId: null,
    });

    setText("");
  };

  return (
    <section className="comments-section">
      <div className="comments-header">
        <h3>
          Комментарии {count?.totalCount ? <span>({count.totalCount})</span> : ""}
        </h3>

        <SortDropdown value={order} onChange={setOrder} />
      </div>

      <div className="comment-form">
        <textarea
            className="comment-textarea"
            placeholder="Написать комментарий…"
            value={text}
            onChange={(e) => {
            setText(e.target.value);
            e.target.style.height = "auto";
            e.target.style.height = e.target.scrollHeight + "px";
  }}
  rows={1}
/>

        <button onClick={submitRoot}>Отправить</button>
      </div>

      {isLoading && <p>Загрузка…</p>}

      {comments.map((c) => (
        <CommentThread key={c.id} comment={c} movieId={movieId} mutations={mutations} />
      ))}
    </section>
  );
}

function CommentThread({ comment, movieId, mutations }) {
  const [open, setOpen] = useState(false);
  const { data } = useReplies(open ? comment.id : null);

  return (
    <div className="comment-thread">
      <CommentItem
        node={comment}
        onReply={(content) =>
          mutations.create.mutate({
            movieId,
            content,
            parentId: comment.id,
          })
        }
        onDelete={() => mutations.remove.mutate(comment.id)}
        onReact={(emoji) => mutations.react.mutate({ id: comment.id, emoji })}
      />

      {comment.repliesCount > 0 && !open && (
        <button className="show-replies" onClick={() => setOpen(true)}>
          Показать ответы ({comment.repliesCount})
        </button>
      )}

      {open &&
        data?.replies?.map((r) => (
          <CommentItem
            key={r.id}
            node={r}
            isReply
            onDelete={() => mutations.remove.mutate(r.id)}
            onReact={(emoji) => mutations.react.mutate({ id: r.id, emoji })}
          />
        ))}
    </div>
  );
}
