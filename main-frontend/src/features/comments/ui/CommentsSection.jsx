import { useEffect, useMemo, useState } from "react";
import CommentItem from "./CommentItem";
import CommentForm from "./CommentForm";
import {
  useRootComments,
  useReplies,
  useCommentCount,
  useCommentMutations,
} from "@/features/comments/model/useComments";
import "@/features/comments/ui/CommentItem.css";
import "./CommentsSection.css"; // ensure we load section CSS

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

      <CommentForm
        value={text}
        onChange={setText}
        onSubmit={submitRoot}
        onCancel={() => setText("")}
        placeholder="Введите комментарий..."
      />

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
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
          Показать ответы ({comment.repliesCount})
        </button>
      )}

      {open && (
        <div className="thread-replies">
          {data?.replies?.map((r) => (
            <CommentItem
              key={r.id}
              node={r}
              isReply
              onDelete={() => mutations.remove.mutate(r.id)}
              onReact={(emoji) => mutations.react.mutate({ id: r.id, emoji })}
            />
          ))}
          <button className="hide-replies" onClick={() => setOpen(false)}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="18 15 12 9 6 15"></polyline></svg>
            Скрыть ответы
          </button>
        </div>
      )}
    </div>
  );
}
