export default function KazakhstanFilters({ genres, filters, onChange }) {
  return (
    <div className="kz-filters" id="kz-filters">
      <select
        id="kz-genre-filter"
        value={filters.genre || ""}
        onChange={(e) =>
          onChange((f) => ({ ...f, genre: e.target.value || null }))
        }
      >
        <option value="">Все жанры</option>
        {genres.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>

      <select
        id="kz-sort-filter"
        value={filters.sortBy}
        onChange={(e) => onChange((f) => ({ ...f, sortBy: e.target.value }))}
      >
        <option value="relevance">По релевантности</option>
        <option value="rating">По рейтингу</option>
        <option value="year">По году</option>
      </select>
    </div>
  );
}
