const GUEST_FAVORITES_KEY = "favorites_guest";
const GUEST_FAVORITES_EVENT = "guest-favorites:changed";

function normalizeFavoriteId(value) {
  if (value === undefined || value === null) {
    return null;
  }

  return String(value);
}

export function readGuestFavorites() {
  try {
    const raw = localStorage.getItem(GUEST_FAVORITES_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed)
      ? parsed.map(normalizeFavoriteId).filter(Boolean)
      : [];
  } catch {
    return [];
  }
}

export function writeGuestFavorites(movieIds) {
  try {
    const normalized = Array.isArray(movieIds)
      ? movieIds.map(normalizeFavoriteId).filter(Boolean)
      : [];
    localStorage.setItem(GUEST_FAVORITES_KEY, JSON.stringify(normalized));
    window.dispatchEvent(
      new CustomEvent(GUEST_FAVORITES_EVENT, {
        detail: { ids: normalized },
      }),
    );
    return normalized;
  } catch {
    return [];
  }
}

export function toggleGuestFavorite(movieId) {
  const normalizedId = normalizeFavoriteId(movieId);
  if (!normalizedId) {
    return { ids: readGuestFavorites(), isFavorite: false };
  }

  const current = readGuestFavorites();
  const exists = current.includes(normalizedId);
  const next = exists
    ? current.filter((value) => value !== normalizedId)
    : [normalizedId, ...current];

  writeGuestFavorites(next);
  return { ids: next, isFavorite: !exists };
}

export function isGuestFavorite(movieId) {
  const normalizedId = normalizeFavoriteId(movieId);
  return normalizedId ? readGuestFavorites().includes(normalizedId) : false;
}

export const guestFavoritesChangedEvent = GUEST_FAVORITES_EVENT;
