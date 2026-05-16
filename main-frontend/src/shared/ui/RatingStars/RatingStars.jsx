import { useMemo } from "react";

const Star = ({
  filled,
  half,
  size = 22,
  onClick,
  onMouseMove,
  onMouseLeave,
  role = "button",
  ariaLabel,
}) => {
  const fill = filled ? "#fbbf24" : half ? "url(#half)" : "none";
  const stroke = filled || half ? "#f59e0b" : "#6b7280";
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      onClick={onClick}
      onMouseMove={onMouseMove}
      onMouseLeave={onMouseLeave}
      role={role}
      aria-label={ariaLabel}
      style={{ cursor: onClick ? "pointer" : "default" }}
    >
      <defs>
        <linearGradient id="half">
          <stop offset="50%" stopColor="#fbbf24" />
          <stop offset="50%" stopColor="transparent" />
        </linearGradient>
      </defs>
      <path
        d="M12 .587l3.668 7.431 8.2 1.193-5.934 5.787 1.401 8.168L12 18.896l-7.335 3.87 1.401-8.168L.132 9.211l8.2-1.193z"
        fill={fill}
        stroke={stroke}
      />
    </svg>
  );
};

export default function RatingStars({
  value = 0,
  onChange,
  readOnly = false,
  size = 22,
}) {
  const stars = useMemo(() => {
    const v = Math.max(0, Math.min(5, value));
    const full = Math.floor(v);
    const half = v - full >= 0.5;
    return { full, half };
  }, [value]);

  const handle = (i, halfStar) => {
    if (!onChange) return;
    const newValue = i + (halfStar ? 0.5 : 1);
    onChange(newValue);
  };

  return (
    <div style={{ display: "flex", gap: 6 }}>
      {[0, 1, 2, 3, 4].map((i) => (
        <div key={i} style={{ position: "relative" }}>
          <Star
            size={size}
            filled={i < stars.full}
            half={i === stars.full && stars.half}
            onClick={
              readOnly
                ? undefined
                : () => handle(i, value - i < 1 && value - i >= 0.5)
            }
            ariaLabel={`Рейтинг ${value} из 5`}
          />
        </div>
      ))}
    </div>
  );
}
