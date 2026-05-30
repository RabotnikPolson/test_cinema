// src/utils/localHistory.js
// Утилита для работы с локальной историей просмотров (localStorage).
// Экспортируется именованная функция useHistoryStorage.

const KEY = 'watch_history_guest_v1';

/**
 * Возвращает API:
 *  - push(item)  : добавить запись { imdbId, title, posterUrl, timestamp }
 *  - read()      : получить массив записей
 *  - clear()     : удалить историю
 */
export function useHistoryStorage() {
  const push = (item) => {
    try {
      if (!item || !item.imdbId) return;
      const raw = localStorage.getItem(KEY);
      const arr = raw ? JSON.parse(raw) : [];

      // удаляем старые записи с тем же imdbId
      const filtered = arr.filter(x => x.imdbId !== item.imdbId);

      // добавляем новую запись в начало (с универсальной структурой)
      const normalized = {
        imdbId: item.imdbId,
        title: item.title || '',
        posterUrl: item.posterUrl || '',
        timestamp: item.timestamp || Date.now()
      };

      filtered.unshift(normalized);

      // ограничиваем длину (например, 50 записей)
      const truncated = filtered.slice(0, 50);
      localStorage.setItem(KEY, JSON.stringify(truncated));
    } catch (e) {
      // не ломаем приложение при ошибке localStorage
      // eslint-disable-next-line no-console
      console.warn('useHistoryStorage.push error', e);
    }
  };

  const read = () => {
    try {
      const raw = localStorage.getItem(KEY);
      return raw ? JSON.parse(raw) : [];
    } catch (e) {
      // eslint-disable-next-line no-console
      console.warn('useHistoryStorage.read error', e);
      return [];
    }
  };

  const clear = () => {
    try {
      localStorage.removeItem(KEY);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.warn('useHistoryStorage.clear error', e);
    }
  };

  return { push, read, clear };
}
