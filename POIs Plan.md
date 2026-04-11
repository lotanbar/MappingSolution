# Google Places + OSM POI Integration Plan

## Context
> Commit done: The 6 recording-logic commits were squashed into one -- "temp upgrade for recording logic" on master.

---

## Google ranking: how does Google decide which POIs to show first?
Google's `searchNearby` ranks by **prominence** by default — a proprietary score based on:
  - Popularity (visit frequency, global search frequency)
  - Reviews count + overall rating
  - Completeness of the Google Business listing
  - Physical importance of the place

This is NOT configurable (no rankPreference needed). The first results from each half-viewport
call will already be the most popular/relevant businesses in that area. This is exactly what we want.

---

## Caching: 7-day TTL (Google), 30-day TTL (OSM)
Both keyed by viewport center lat/lng rounded to 2dp (~1km grid cells), JSON on disk in cacheDir.
OSM data never moves (mountain peaks don't relocate), so a much longer TTL is safe and sensible.

### Cache logic per refresh (Google):
  1. Compute cache key from viewport center (lat2dp + "_" + lng2dp)
  2. If cache file exists AND age < 7 days: load cached POIs, filter to current viewport bounds
  3. cached_count = valid cached POIs within viewport
  4. remaining = max(0, GOOGLE_MAX_RESULTS - cached_count)
  5. If remaining = 0: skip server fetch, emit cached list
  6. If remaining > 0:
       - If remaining is odd: remaining_rounded = remaining + 1 (e.g., 25 → 26)
       - Each N/S half requests: remaining_rounded / 2 (e.g., 26 → 13+13)
       - maxResultCount = ceil(remaining_rounded / 2), capped at 20 (API hard limit)
       - Fetch from server, dedupe with cached by placeId
       - Store combined result back to cache file with current timestamp
  7. Emit final deduped list

### Cache logic per refresh (OSM):
  1. Same cache key lookup
  2. If cache hit and age < 30 days: emit cached POIs directly (no server call)
  3. If miss/stale: fetch ALL matching nodes from server (no count limit), store to cache, emit

### OSM: no fixed count limit
  The Overpass query uses `out body;` (no MAX_RESULTS cap).
  Natural features are sparse enough that flooding the map isn't a concern.
  Show everything matching the query for the current viewport.

### Cache eviction (on every app launch):
  - Scan all files in cacheDir matching "gp_*.json" and "osm_*.json"
  - Parse fetchedAt timestamp from each
  - If Google cache (gp_*) and age > 7 days: delete file
  - If OSM cache (osm_*) and age > 30 days: delete file
  - NO refetch — just delete. User naturally gets fresh data when visiting that viewport again.

---

## Data Sources: Split by permanence

### Google Places API (New) -- Businesses
Fetches commercially-operated, user-facing venues. Google's strengths are business hours,
ratings, phone numbers -- all things that change and are actively maintained by businesses.

Scope: filter to explicit includedTypes list (constant, easy to expand):
  restaurant, cafe, bar, bakery, fast_food_restaurant, coffee_shop,
  bank, atm,
  pharmacy, hospital, doctor, dentist,
  supermarket, shopping_mall, clothing_store, convenience_store,
  hotel, lodging, hostel,
  gas_station,
  gym, beauty_salon, hair_salon,
  movie_theater, night_club

Rationale for exclusions:
  Google has national_park, wildlife_refuge, etc. in its types -- but coverage is poor
  because these don't actively claim Google Business listings. OSM is far better for them.

API: POST https://places.googleapis.com/v1/places:searchNearby
  - Field mask: places.id, places.displayName, places.location (Pro SKU only -- cheapest)
  - includedTypes: (list above)
  - Target: 30 POIs total per viewport (15+15 default, adjusted for cache, see Cache section)
  - maxResultCount: 15 per half by default; reduced dynamically when cache supplies some
  - Hard API limit = 20 per call (we never exceed it)
  - Requires GOOGLE_PLACES_API_KEY

### OpenStreetMap via Overpass API -- Natural & Permanent
Free, no API key, community-maintained. Excellent coverage of:
  - Mountain peaks / volcanos / glaciers
  - Nature reserves
  - Astronomical observatories
  - Waterfalls, caves, hot springs
  - Historic monuments, castles, ruins, archaeological sites
  - Scenic viewpoints
  - Lighthouses

API: POST https://overpass-api.de/api/interpreter
  - No auth required
  - Response format: JSON
  - Query uses bounding box (full viewport, no split needed -- returns all matching nodes)

Overpass QL query template (bbox-based, nodes with names only):
  [out:json][timeout:25][bbox:{south},{west},{north},{east}];
  (
    node[natural~"^(peak|volcano|cave_entrance|waterfall|glacier|hot_spring|geyser)$"][name];
    node[leisure=nature_reserve][name];
    node[amenity=observatory][name];
    node[historic~"^(monument|castle|archaeological_site|ruins|fort|memorial)$"][name];
    node[tourism=viewpoint][name];
    node[man_made=lighthouse][name];
  );
  out body;
  (no count limit -- show all matching nodes for the viewport)

OSM Response -> Poi mapping:
  id = "osm_" + element.id (string)
  name = element.tags.name (or tags["name:en"] if no default name)
  lat = element.lat
  lng = element.lon
  groupId = OSM_POI_GROUP_ID (reserved constant)
  description = null, elevation = null, mediaPaths = [], isVisible = true
  createdAt = fetch timestamp

---

## Existing Poi field mapping (both sources)

| Poi field     | Google Places              | OSM                         |
|---------------|----------------------------|-----------------------------|
| id            | places[].id (placeId)      | "osm_" + element.id         |
| name          | places[].displayName.text  | element.tags.name           |
| lat/lng       | places[].location          | element.lat / element.lon   |
| groupId       | GOOGLE_PLACES_GROUP_ID     | OSM_POI_GROUP_ID            |
| description   | null                       | null                        |
| elevation     | null                       | null                        |
| mediaPaths    | []                         | []                          |

No changes to Poi.kt.

---

## Constants file: data/places/PlacesConstants.kt

```kotlin
const val GOOGLE_PLACES_GROUP_ID = "google-places-group"
const val OSM_POI_GROUP_ID = "osm-poi-group"

// Zoom threshold -- adjust as needed, applies to BOTH sources
const val NEARBY_POI_MIN_ZOOM = 13.0

const val GOOGLE_PLACES_FETCH_DEBOUNCE_MS = 800L
const val GOOGLE_PLACES_MAX_RESULTS = 30  // target per viewport; fetched as 15+15 or adjusted for cache
const val GOOGLE_PLACES_MAX_PER_CALL = 20 // hard API limit per single call
const val GOOGLE_PLACES_CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000   // 7 days
const val GOOGLE_PLACES_FIELD_MASK = "places.id,places.displayName,places.location"

val GOOGLE_PLACES_INCLUDED_TYPES = listOf(
    "restaurant", "cafe", "bar", "bakery", "fast_food_restaurant", "coffee_shop",
    "bank", "atm",
    "pharmacy", "hospital",
    "supermarket", "shopping_mall", "convenience_store",
    "hotel", "lodging",
    "gas_station",
    "gym", "beauty_salon"
)

const val OSM_FETCH_DEBOUNCE_MS = 800L
// No OSM_MAX_RESULTS -- OSM query uses `out body;` (uncapped)
const val OSM_CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000   // 30 days
```

---

## Architecture

Two completely separate repositories, both injected via Hilt, both in-memory only.
Each has its own Group seeded in GroupFileRepository (isImported=true -> uncollapsable in Library).

Map layer stack (bottom to top):
  saved-routes-lines          (existing)
  live-route-line             (existing)
  osm-poi-symbols             (new -- green pins, #4CAF50)
  google-places-symbols       (new -- blue pins, #4285F4)
  poi-symbols                 (existing -- user POIs, on top)

Tap handler priority: user POIs > Google Places > OSM POIs > routes

---

## Implementation Phases + Commit Strategy

> Recommended: one commit per phase. Each phase is independently buildable and testable on device.

### Commit 1 -- POI data layer (Phase 0 + Phase 1)
Prerequisites + all data layer code. Nothing visible to the user yet.
Manual test: build succeeds, no crashes on launch, cache eviction runs silently.

  Phase 0 -- Prerequisites:
    local.properties          add GOOGLE_PLACES_API_KEY=...
    libs.versions.toml        add okhttp version
    build.gradle.kts          OkHttp dep + BuildConfig.GOOGLE_PLACES_API_KEY field

  Phase 1 -- Data layer (create):
    data/places/PlacesConstants.kt
    data/places/PlacesApiService.kt     (Google, splits N/S, cache-aware maxPerHalf)
    data/places/OsmApiService.kt        (OSM, full bbox, out body;)
    data/places/GooglePlacesCache.kt    (disk cache, 7-day TTL, gp_{key}.json)
    data/places/OsmPoiCache.kt          (disk cache, 30-day TTL, osm_{key}.json)
    data/places/GooglePlacesRepository.kt
    data/places/OsmPoiRepository.kt
    di/NetworkModule.kt

  Phase 1 -- Data layer (modify):
    data/fs/GroupFileRepository.kt      seed both groups on first run

### Commit 2 -- Library integration (Phase 2)
Both new group rows visible in Library with eye toggles. Counts update live.
Manual test: open Library -> see "Google Places" + "OpenStreetMap POIs" rows -> eye toggle works.

  ui/library/LibraryViewModel.kt        inject both repos, expose counts + toggles
  ui/library/LibraryScreen.kt           protect both group rows from selection/deletion

### Commit 3 -- Map integration (Phase 3)
POIs appear on map when zoomed in. Pins update on camera move. Tapping a pin navigates (to empty screen for now).
Manual test: zoom past 13 -> blue/green pins appear -> pan/zoom triggers refetch -> tap pin navigates.

  ui/main/MainViewModel.kt              inject both repos, onCameraChanged, cache eviction on init
  ui/main/MainScreen.kt                 collect + pass 4 flows, wire both tap callbacks
  ui/main/components/MapComponent.kt    two new layers, extended onCameraIdle, tap priority

### Commit 4 -- Detail screen read-only (Phase 4)
Tapping a Google/OSM pin shows the detail screen. Edit is grayed. Delete is hidden.
Manual test: tap Google pin -> detail shows name + group + location -> Edit is grayed + toasts -> Delete is hidden.

  ui/detail/ItemDetailViewModel.kt      handle google_place + osm_poi types, isReadOnly flag
  ui/detail/ItemDetailScreen.kt         isReadOnly rendering (Edit disabled+toast, Delete hidden)

---

## Files Summary

Create (8 files):
  data/places/PlacesConstants.kt
  data/places/PlacesApiService.kt
  data/places/OsmApiService.kt
  data/places/GooglePlacesCache.kt
  data/places/OsmPoiCache.kt
  data/places/GooglePlacesRepository.kt
  data/places/OsmPoiRepository.kt
  di/NetworkModule.kt

Modify (11 files):
  local.properties                      add GOOGLE_PLACES_API_KEY
  build.gradle.kts                      OkHttp dep + BuildConfig field
  libs.versions.toml                    OkHttp version
  data/fs/GroupFileRepository.kt        seed both groups on first run
  ui/main/MainViewModel.kt              inject both repos, onCameraChanged, expose flows
  ui/main/MainScreen.kt                 collect + pass all 4 flows, wire both taps
  ui/main/components/MapComponent.kt    two new layers, extended onCameraIdle, tap priority
  ui/library/LibraryViewModel.kt        inject both repos, expose counts + toggles
  ui/library/LibraryScreen.kt           protect both group rows from selection/deletion
  ui/detail/ItemDetailViewModel.kt      handle both new types, isReadOnly flag
  ui/detail/ItemDetailScreen.kt         isReadOnly rendering

---

## Commit Order

- commit_1:
    title: POI data layer + group seeding
    scope:
      - Add GOOGLE_PLACES_API_KEY to local.properties + BuildConfig
      - Add OkHttp 4.12.0 to libs.versions.toml + build.gradle.kts
      - PlacesConstants.kt (all tunables)
      - PlacesApiService.kt (Google, N/S split, cache-aware maxPerHalf)
      - OsmApiService.kt (Overpass QL, full bbox, out body;)
      - GooglePlacesCache.kt (disk cache, 7-day TTL)
      - OsmPoiCache.kt (disk cache, 30-day TTL)
      - GooglePlacesRepository.kt (Hilt singleton, refreshForViewport, evictStaleCacheOnLaunch)
      - OsmPoiRepository.kt (Hilt singleton, refreshForViewport, evictStaleCacheOnLaunch)
      - NetworkModule.kt (Hilt @Module for all new singletons)
      - GroupFileRepository.kt: seed Google Places + OSM groups on first run
    manual_test:
      - Build succeeds, app launches without crashes
      - On first launch: check logcat for group seeding ("Google Places" + "OpenStreetMap POIs" groups created)
      - Cache eviction logs run silently on launch
    edge_and_error_tests:
      - No network on launch: repos emit empty list, no crash
      - Missing/invalid API key: Google calls fail gracefully (log error, emit empty)
      - Cache directory not writable: fallback to always-fetch, no crash

- commit_2:
    title: Library rows for Google Places + OSM
    scope:
      - LibraryViewModel: inject both repos, expose googlePlaceCount, osmPoiCount, toggles
      - LibraryScreen: show both group rows with live counts, eye toggle, protected from selection/deletion
    manual_test:
      - Open Library: "Google Places" (blue) and "OpenStreetMap POIs" (green) rows visible
      - Eye toggle on either row updates map visibility
      - Long-press on either row: selection mode does NOT include these rows
      - Delete action cannot target these groups
    edge_and_error_tests:
      - Count stays 0 until user zooms to threshold and pans -- verify no stale count
      - Toggle visibility while count = 0: no crash, layer hides gracefully

- commit_3:
    title: Map integration -- pins, fetch on camera move, tap navigation
    scope:
      - MapComponent: two new GeoJsonSources + SymbolLayers (blue/green), extended onCameraIdle with LatLngBounds, tap priority
      - MainViewModel: inject both repos, onCameraChanged (debounced refresh + camera save), evict cache on init
      - MainScreen: collect 4 flows, pass to MapComponent, wire both tap callbacks to navigation
    manual_test:
      - Zoom past 13: blue Google pins + green OSM pins appear on map
      - Pan/zoom: debounce fires, new pins appear after ~800ms idle
      - Zoom below 13: pins disappear
      - Tap Google pin: navigates to item_detail/google_place/{id}
      - Tap OSM pin: navigates to item_detail/osm_poi/{id}
      - Tap user POI pin: still works (priority preserved)
      - Eye toggle from Library: respective layer hides/shows on map
    edge_and_error_tests:
      - Rapid panning: debounce cancels in-flight jobs correctly, no duplicate fetches
      - Tapping overlapping pins: user POI wins over Google, Google wins over OSM
      - Network error mid-pan: previous pins remain, no crash

- commit_4:
    title: Read-only detail screen for Google Places + OSM POIs
    scope:
      - DetailItem.PoiDetail: add isReadOnly: Boolean = false
      - ItemDetailViewModel: handle "google_place" + "osm_poi" types in load(); set isReadOnly=true
      - ItemDetailScreen: isReadOnly param -- Edit button disabled+gray (tap → Toast), Delete button hidden
    manual_test:
      - Tap Google pin -> detail screen shows: name, "Google Places" group, lat/lng location, fetch date
      - Edit button visible but grayed out; tapping it shows a Toast ("Cannot edit Google Places POI")
      - Delete button is hidden
      - Tap OSM pin -> same behavior with "OpenStreetMap POIs" group
      - Tap user POI -> Edit + Delete still work normally (isReadOnly=false)
    edge_and_error_tests:
      - Navigate to detail after repo cleared (user zoomed far away): show empty/not-found state gracefully
      - Back navigation from read-only detail returns to map correctly

