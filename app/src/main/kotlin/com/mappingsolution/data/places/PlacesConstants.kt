package com.mappingsolution.data.places

const val GOOGLE_PLACES_GROUP_ID = "google-places-group"
const val OSM_POI_GROUP_ID = "osm-poi-group"

// Zoom threshold — applies to BOTH sources
const val NEARBY_POI_MIN_ZOOM = 13.0

const val GOOGLE_PLACES_FETCH_DEBOUNCE_MS = 800L
const val GOOGLE_PLACES_MAX_RESULTS = 30     // target per viewport; fetched as 15+15 or adjusted for cache
const val GOOGLE_PLACES_MAX_PER_CALL = 20    // hard API limit per single call
const val GOOGLE_PLACES_CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000   // 7 days
const val GOOGLE_PLACES_FIELD_MASK = "places.id,places.displayName,places.location"

val GOOGLE_PLACES_INCLUDED_TYPES = listOf(
    "restaurant", "cafe", "bar", "bakery", "fast_food_restaurant", "coffee_shop",
    "bank", "atm",
    "pharmacy", "hospital",
    "supermarket", "shopping_mall", "convenience_store",
    "hotel", "lodging",
    "gas_station",
    "gym", "beauty_salon",
)

const val OSM_FETCH_DEBOUNCE_MS = 800L
// No OSM_MAX_RESULTS — OSM query uses `out body;` (uncapped)
const val OSM_CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000   // 30 days
