import React, { useRef } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { FreeMode, Mousewheel, Navigation } from "swiper/modules";
import { Swiper, SwiperSlide } from "swiper/react";
import "swiper/css";
import "swiper/css/navigation";
import "./MovieCarousel.css";

const DEFAULT_BREAKPOINTS = {
  320: { slidesPerView: 1.2, spaceBetween: 12 },
  560: { slidesPerView: 2.2, spaceBetween: 14 },
  900: { slidesPerView: 3.2, spaceBetween: 16 },
  1200: { slidesPerView: 4.2, spaceBetween: 18 },
  1440: { slidesPerView: 5, spaceBetween: 18 },
};

export default function MovieCarousel({
  items,
  renderSlide,
  keyExtractor,
  breakpoints = DEFAULT_BREAKPOINTS,
  spaceBetween = 18,
}) {
  const prevRef = useRef(null);
  const nextRef = useRef(null);

  if (!items?.length) return null;

  return (
    <div className="movie-carousel-wrap">
      <button
        ref={prevRef}
        type="button"
        className="movie-carousel-btn"
        aria-label="Назад"
      >
        <ChevronLeft size={20} />
      </button>

      <Swiper
        className="movie-carousel"
        modules={[Navigation, Mousewheel, FreeMode]}
        navigation={{
          prevEl: prevRef.current,
          nextEl: nextRef.current,
        }}
        onBeforeInit={(swiper) => {
          swiper.params.navigation.prevEl = prevRef.current;
          swiper.params.navigation.nextEl = nextRef.current;
        }}
        mousewheel={{ forceToAxis: true, releaseOnEdges: true, sensitivity: 1 }}
        freeMode={{ enabled: true, momentum: true, momentumRatio: 0.5, momentumVelocityRatio: 0.5 }}
        slidesPerView={1.2}
        spaceBetween={spaceBetween}
        breakpoints={breakpoints}
      >
        {items.map((item, i) => (
          <SwiperSlide key={keyExtractor(item, i)}>
            {renderSlide(item)}
          </SwiperSlide>
        ))}
      </Swiper>

      <button
        ref={nextRef}
        type="button"
        className="movie-carousel-btn"
        aria-label="Вперёд"
      >
        <ChevronRight size={20} />
      </button>
    </div>
  );
}