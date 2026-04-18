package com.mappingsolution.data.places

object PoiIconResolver {

    // ── Google Places direct map ──────────────────────────────────────────────

    private val GOOGLE_TYPE_TO_ICON = mapOf(
        "restaurant"           to "restaurant",
        "cafe"                 to "local_cafe",
        "bar"                  to "local_bar",
        "bakery"               to "bakery_dining",
        "fast_food_restaurant" to "fastfood",
        "coffee_shop"          to "coffee",
        "pizza_restaurant"     to "local_pizza",
        "sandwich_shop"        to "fastfood",
        "ice_cream_shop"       to "icecream",
        "dessert_shop"         to "cake",
        "wine_bar"             to "wine_bar",
        "brewery"              to "local_bar",
        "bank"                 to "account_balance",
        "atm"                  to "local_atm",
        "pharmacy"             to "local_pharmacy",
        "hospital"             to "local_hospital",
        "doctor"               to "local_hospital",
        "dentist"              to "local_hospital",
        "veterinary_care"      to "local_hospital",
        "supermarket"          to "shopping_cart",
        "grocery_store"        to "shopping_cart",
        "shopping_mall"        to "shopping_cart",
        "clothing_store"       to "storefront",
        "convenience_store"    to "storefront",
        "book_store"           to "storefront",
        "electronics_store"    to "storefront",
        "hardware_store"       to "storefront",
        "furniture_store"      to "storefront",
        "hotel"                to "hotel",
        "lodging"              to "hotel",
        "hostel"               to "night_shelter",
        "campground"           to "night_shelter",
        "gas_station"          to "local_gas_station",
        "electric_vehicle_charging_station" to "electric_car",
        "parking"              to "local_parking",
        "gym"                  to "fitness_center",
        "beauty_salon"         to "spa",
        "hair_salon"           to "spa",
        "spa"                  to "spa",
        "movie_theater"        to "theaters",
        "night_club"           to "nightlife",
        "casino"               to "casino",
        "museum"               to "museum",
        "art_gallery"          to "museum",
        "library"              to "museum",
        "park"                 to "park",
        "national_park"        to "nature",
        "zoo"                  to "nature",
        "aquarium"             to "pool",
        "amusement_park"       to "attractions",
        "tourist_attraction"   to "attractions",
        "stadium"              to "sports_soccer",
        "sports_club"          to "sports_soccer",
        "golf_course"          to "golf_course",
        "bowling_alley"        to "sports_soccer",
        "school"               to "school",
        "university"           to "school",
        "airport"              to "flight",
        "train_station"        to "train",
        "subway_station"       to "train",
        "bus_station"          to "directions_bus",
        "ferry_terminal"       to "anchor",
        "marina"               to "anchor",
        "police"               to "local_police",
        "fire_station"         to "local_fire_department",
        "church"               to "bookmark",
        "mosque"               to "bookmark",
        "synagogue"            to "bookmark",
        "hindu_temple"         to "bookmark",
        "place_of_worship"     to "bookmark",
        "cemetery"             to "favorite",
        "monument"             to "tour",
        "historical_landmark"  to "tour",
        "car_dealer"           to "directions_car",
        "car_rental"           to "directions_car",
        "car_repair"           to "directions_car",
        "laundry"              to "local_laundry",
        "post_office"          to "work",
        "real_estate_agency"   to "apartment",
        "accounting"           to "business_center",
        "lawyer"               to "business_center",
        "insurance_agency"     to "business_center",
    )

    // ── OSM tag-value direct map ───────────────────────────────────────────────

    private val OSM_NATURAL_TO_ICON = mapOf(
        "peak"           to "terrain",
        "volcano"        to "terrain",
        "cave_entrance"  to "landscape",
        "waterfall"      to "waves",
        "glacier"        to "ac_unit",
        "hot_spring"     to "water_drop",
        "geyser"         to "water_drop",
    )

    private val OSM_HISTORIC_TO_ICON = mapOf(
        "monument"            to "tour",
        "memorial"            to "favorite",
        "castle"              to "flag",
        "fort"                to "flag",
        "fortification"       to "flag",
        "city_gate"           to "flag",
        "archaeological_site" to "museum",
        "ruins"               to "museum",
        "building"            to "museum",
        "manor"               to "museum",
        "place_of_worship"    to "bookmark",
        "wayside_shrine"      to "bookmark",
        "wayside_cross"       to "bookmark",
        "tomb"                to "favorite",
        "milestone"           to "tour",
        "boundary_stone"      to "tour",
        "ship"                to "anchor",
        "aircraft"            to "flight",
        "tank"                to "flag",
        "cannon"              to "flag",
        "battlefield"         to "favorite",
    )

    private val OSM_OTHER_TO_ICON = mapOf(
        // leisure
        "nature_reserve"  to "nature",
        "park"            to "park",
        "garden"          to "park",
        "playground"      to "park",
        "sports_centre"   to "sports_soccer",
        "swimming_pool"   to "pool",
        "golf_course"     to "golf_course",
        "marina"          to "anchor",
        "beach"           to "beach_access",
        "picnic_table"    to "park",
        "dog_park"        to "park",
        // amenity
        "observatory"     to "satellite",
        "restaurant"      to "restaurant",
        "cafe"            to "local_cafe",
        "bar"             to "local_bar",
        "pub"             to "local_bar",
        "fast_food"       to "fastfood",
        "food_court"      to "restaurant",
        "hospital"        to "local_hospital",
        "clinic"          to "local_hospital",
        "doctors"         to "local_hospital",
        "dentist"         to "local_hospital",
        "pharmacy"        to "local_pharmacy",
        "school"          to "school",
        "university"      to "school",
        "college"         to "school",
        "bank"            to "account_balance",
        "atm"             to "local_atm",
        "fuel"            to "local_gas_station",
        "parking"         to "local_parking",
        "police"          to "local_police",
        "fire_station"    to "local_fire_department",
        "theatre"         to "theaters",
        "cinema"          to "theaters",
        "museum"          to "museum",
        "library"         to "museum",
        "place_of_worship" to "bookmark",
        "bus_station"     to "directions_bus",
        "taxi"            to "local_taxi",
        "ferry_terminal"  to "anchor",
        "bicycle_rental"  to "directions_bike",
        "car_rental"      to "directions_car",
        "supermarket"     to "shopping_cart",
        "marketplace"     to "storefront",
        "laundry"         to "local_laundry",
        // tourism
        "viewpoint"       to "visibility",
        "hotel"           to "hotel",
        "hostel"          to "night_shelter",
        "camp_site"       to "night_shelter",
        "caravan_site"    to "night_shelter",
        "attraction"      to "attractions",
        "gallery"         to "museum",
        "information"     to "info",
        "picnic_site"     to "park",
        "wilderness_hut"  to "night_shelter",
        "alpine_hut"      to "night_shelter",
        // man_made
        "lighthouse"      to "anchor",
        "windmill"        to "tour",
        "water_tower"     to "water_drop",
        "tower"           to "visibility",
    )

    // ── Imported GPX fuzzy keyword table (priority-ordered) ───────────────────
    // Each entry: list of keywords → iconKey. First substring match wins.

    private val IMPORT_KEYWORD_TABLE: List<Pair<List<String>, String>> = listOf(
        listOf("beach", "shore", "coast", "seaside")                                  to "beach_access",
        listOf("mountain", "peak", "summit", "hill", "ridge", "cliff", "highland")    to "terrain",
        listOf("waterfall", "cascade", "rapids")                                      to "waves",
        listOf("cave", "cavern", "grotto")                                            to "landscape",
        listOf("glacier", "icefield")                                                 to "ac_unit",
        listOf("hot spring", "thermal", "geyser")                                     to "water_drop",
        listOf("volcano")                                                              to "terrain",
        listOf("viewpoint", "overlook", "lookout", "vista", "panorama", "scenic")    to "visibility",
        listOf("lighthouse")                                                           to "anchor",
        listOf("castle", "fort", "fortress", "citadel")                              to "flag",
        listOf("ruin", "ruins", "ancient", "archaeological", "archaeology")           to "museum",
        listOf("monument", "obelisk", "landmark", "pillar")                          to "tour",
        listOf("memorial", "grave", "cemetery", "tombstone", "mausoleum")            to "favorite",
        listOf("statue", "sculpture")                                                 to "attractions",
        listOf("museum", "gallery", "exhibit", "exhibition")                         to "museum",
        listOf("church", "cathedral", "chapel", "mosque", "temple", "synagogue", "shrine") to "bookmark",
        listOf("restaurant", "diner", "eatery", "bistro", "grill")                   to "restaurant",
        listOf("cafe", "coffee", "espresso")                                          to "local_cafe",
        listOf("bar", "pub", "tavern", "brewery", "saloon")                          to "local_bar",
        listOf("bakery", "pastry")                                                    to "bakery_dining",
        listOf("fast food", "fastfood", "burger", "pizza", "sandwich")               to "fastfood",
        listOf("hotel", "motel", "inn", "resort", "lodge", "accommodation")          to "hotel",
        listOf("hostel", "campsite", "camp", "tent")                                 to "night_shelter",
        listOf("hospital", "clinic", "medical", "health center")                     to "local_hospital",
        listOf("pharmacy", "drugstore", "chemist")                                   to "local_pharmacy",
        listOf("school", "university", "college", "academy", "campus")               to "school",
        listOf("park", "garden", "playground")                                       to "park",
        listOf("forest", "woods", "woodland", "jungle")                              to "forest",
        listOf("nature", "reserve", "wildlife", "sanctuary", "habitat")              to "nature",
        listOf("observatory")                                                         to "satellite",
        listOf("gas", "fuel", "petrol", "diesel", "refuel")                          to "local_gas_station",
        listOf("parking", "car park")                                                to "local_parking",
        listOf("atm", "cash machine")                                                to "local_atm",
        listOf("bank", "finance", "credit union")                                    to "account_balance",
        listOf("shop", "store", "market", "mall", "shopping", "supermarket")        to "storefront",
        listOf("airport", "terminal", "aviation", "airfield")                       to "flight",
        listOf("train", "railway", "station", "rail")                               to "train",
        listOf("bus", "transit", "stop")                                             to "directions_bus",
        listOf("taxi", "cab")                                                        to "local_taxi",
        listOf("marina", "port", "harbor", "harbour", "dock", "pier", "jetty")      to "anchor",
        listOf("ski", "skiing", "snowboard", "slope", "piste")                      to "downhill_skiing",
        listOf("golf")                                                               to "golf_course",
        listOf("gym", "fitness", "workout", "crossfit")                             to "fitness_center",
        listOf("swimming", "pool", "aquatic")                                       to "pool",
        listOf("surfing", "surf", "wave")                                           to "surfing",
        listOf("kayak", "canoe", "rafting")                                         to "kayaking",
        listOf("sailing", "yacht")                                                  to "sailing",
        listOf("paragliding", "hang gliding", "gliding")                           to "paragliding",
        listOf("trail start", "trail end", "trailhead", "waypoint", "navigation point") to "navigation",
        listOf("trail junction", "trail fork", "intersection", "crossroads")          to "near_me",
        listOf("hiking", "trail", "trekking", "trek", "walk path")                   to "hiking",
        listOf("cycling", "bicycle", "bike")                                        to "directions_bike",
        listOf("fire station", "firehouse")                                         to "local_fire_department",
        listOf("police", "security", "law enforcement")                             to "local_police",
        listOf("concert", "music", "venue")                                         to "music_note",
        listOf("theater", "theatre", "cinema", "movie")                            to "theaters",
        listOf("nightclub", "club", "disco", "lounge")                             to "nightlife",
        listOf("casino", "gambling")                                               to "casino",
        listOf("stadium", "arena", "sports complex")                               to "sports_soccer",
        listOf("home", "house", "residence", "villa")                              to "home",
        listOf("apartment", "flat", "building", "office")                          to "apartment",
        listOf("laundry", "dry clean")                                             to "local_laundry",
    )

    // ── Imported GPX fuzzy keyword table — Hebrew ────────────────────────────
    // Covers common hiking/trail POI terminology in Israeli GPX exports.
    // Words are matched as substrings (case doesn't apply to Hebrew).

    private val IMPORT_HEBREW_KEYWORD_TABLE: List<Pair<List<String>, String>> = listOf(
        // Burial caves — must be before generic cave/water so they win
        listOf("מערת קבורה", "מערת קבר", "מערות קבורה", "מערת קבורות") to "favorite",

        // Terrain / topography
        // NOTE: "הר" alone EXCLUDED — it is a substring of "הרוס" (ruins) and "נהר" (river)
        listOf("פסגה", "מצוק", "גבעה", "ראש ההר", "ראש", "מעלה", "מורד", "רכס", "גב הר", "גב") to "terrain",
        listOf("מעבר", "צוואר", "אוכף") to "navigation",
        listOf("עמק", "גיא", "קניון") to "landscape",

        // Water features
        listOf("מפל", "מפלים", "מפלון") to "waves",
        listOf("נחל") to "waves",
        listOf("ואדי") to "waves",
        listOf("סכר") to "waves",  // dam / weir
        // NOTE: "ים" alone EXCLUDED — it is the Hebrew plural suffix (שרידים, קברים, etc.)
        listOf("חוף ים", "חוף הים", "חוף", "ים המלח", "כנרת", "ים סוף") to "beach_access",
        // "בור" alone catches cisterns: בור פעמון, בור סיד, בור חצוב, etc.
        listOf("מעיין", "מעין", "עין", "גבים", "נביעה",
               "בור מים", "בריכת מים", "בריכת", "בורות מים", "בורות",
               "בור", "באר", "באר מים", "מאגר מים", "ברז מים", "ציר מים",
               "מגדל מים", "מגדל המים", "מעביר מים", "מעבר מים") to "water_drop",
        listOf("אגם", "ברכה", "בריכה", "בריכת חורף", "שלולית") to "pool",
        listOf("ביצה", "אזור לח") to "nature",

        // Caves — after burial cave entry; "מערת" catches construct form (מערת התאנה etc.)
        listOf("מערה", "מחילה", "נקיק", "מערות", "מערת", "מערונת", "פתח מערה") to "landscape",

        // Flora / trees / flowers
        listOf("יער", "חורש", "שיטה", "שיטים", "אורן", "שיזף", "אלה", "חרוב",
               "אלון", "אשל", "עץ שיטה", "עץ שיזף", "עץ חרוב", "חורשת") to "forest",
        listOf("שמורה", "גן לאומי", "גן טבע", "שמורת טבע") to "nature",
        // Flowers — "אירוס" catches אירוסים, אירוס שחום, אירוס הסרגל; "חלמון" catches חלמוניות
        listOf("פרח", "פרחים", "פריחה", "איריס", "אירוס", "כלנית", "נרקיס",
               "חלמון", "חצב", "צבעון", "בולבוס") to "filter_vintage",

        // Viewpoints / observation towers
        listOf("תצפית", "נקודת תצפית", "מצפה", "מצפור") to "visibility",
        listOf("שומרה", "מגדל תצפית", "מגדל שמירה", "מגדל שדה", "מגדל") to "visibility",

        // Danger / warnings
        listOf("מוקש", "מוקשי", "שדה מוקשים", "אין מעבר", "חסימה", "בורות פתוחים",
               "מחסום", "תהום", "סכנת", "דרך חסומה", "דרך משובשת",
               "מעקה שבור", "מזבלה") to "warning",

        // Military sites
        listOf("בונקר", "מוצב", "מחפורת", "עמדת שמירה") to "flag",
        listOf("מצודה", "מבצר", "חומה") to "flag",

        // Historical / archaeological
        // "הרוס" is here explicitly (not under terrain) to beat "הר" false-match concern
        listOf("הרוס", "חורבת", "חורבה", "חרבה", "חירבה", "שרידים", "שרידי",
               "עתיקות", "אתר עתיקות", "עתיק", "עתיקה") to "museum",
        listOf("ארכיאולוג", "ממצאים", "מאובנים", "מחצבה",
               "ציורי סלע", "כתובת", "פסיפס", "קו העתק") to "museum",
        listOf("תל ") to "museum",  // Tel (archaeological mound) — trailing space avoids mid-word
        listOf("גת", "כבשן", "בית בד") to "museum",  // wine press, lime kiln, olive press
        listOf("מטמורה", "מטמורות", "קולומבריום", "כוך", "ספלול", "מאגורה",
               "מבנה", "מבנה נטוש", "מבנה אבן", "מבנה עתיק") to "museum",
        listOf("טחנת", "גשר", "גשרון", "טראסות", "מדרגה", "מלכודת") to "museum",  // mill, bridge, terraces
        listOf("דרך רומית", "דרך עתיקה", "דרך ביזנטית") to "museum",

        // Religious sites — bookmark (not white-dot "place")
        listOf("מזבח", "פולחן", "בית מקדש") to "bookmark",
        listOf("כנסייה", "קתדרלה", "כנסיה") to "bookmark",
        listOf("מסגד") to "bookmark",
        listOf("מקדש") to "bookmark",
        listOf("בית כנסת", "בית-כנסת", "כנסת") to "bookmark",  // "כנסת" catches "בית הכנסת"

        // Cemeteries / burial mounds / memorials
        listOf("טומולי", "טומולוס", "קבר ארגז") to "favorite",  // burial mounds — before generic קבר
        listOf("קבר", "מצבה", "קברים", "בית קברות", "עלמין", "בית עלמין") to "favorite",
        listOf("אנדרטה", "הנצחה", "שלט הנצחה", "יד לחיילים", "מצבת", "אנדרטת") to "tour",

        // Gates / entrances (historical)
        listOf("שער", "כניסה") to "tour",

        // Trails / navigation — split to reduce "walking guy" overuse
        listOf("נקודת התחלה", "נקודת סיום", "ניווט", "נקודה") to "navigation",
        listOf("פיצול שבילים", "צומת שבילים", "מפגש שבילים", "מפגש",
               "פיצול", "צומת", "מסעף") to "near_me",
        listOf("סימן דרך", "סימן ") to "hiking",  // trail marker/sign
        listOf("שביל", "מסלול", "תוואי", "נקודת חובה") to "hiking",

        // Parks / recreation areas
        listOf("פיקניק", "שולחן פיקניק", "שולחנות", "אזור פיקניק", "פינת ישיבה", "בוסתן") to "park",
        listOf("גן", "פארק", "גינה") to "park",

        // Food & drink
        listOf("מסעדה") to "restaurant",
        listOf("קפה", "בית קפה") to "local_cafe",

        // Services
        listOf("תחנת דלק", "תדלוק") to "local_gas_station",
        listOf("חניה", "חניון") to "local_parking",
        listOf("בית חולים", "קופת חולים", "מרפאה") to "local_hospital",
        listOf("בית ספר", "אוניברסיטה", "מכללה") to "school",

        // Accommodation
        listOf("מלון", "בית הארחה", "אכסניה") to "hotel",
        listOf("קמפינג", "אוהל", "קמפ") to "night_shelter",
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves an icon key from a Google Places type string.
     * Iterates the types array (caller passes each element) until a known type is matched.
     * Returns "place" as fallback.
     */
    fun resolveForGoogleType(types: List<String>): String {
        for (type in types) {
            val icon = GOOGLE_TYPE_TO_ICON[type]
            if (icon != null) return icon
        }
        return "place"
    }

    /**
     * Resolves an icon key from an OSM tags map.
     * Checks natural → historic → leisure/amenity/tourism/man_made keys in order.
     * Returns "place" as fallback.
     */
    fun resolveForOsmTags(tags: Map<String, String>): String {
        tags["natural"]?.let { OSM_NATURAL_TO_ICON[it] }?.let { return it }
        tags["historic"]?.let { OSM_HISTORIC_TO_ICON[it] }?.let { return it }
        tags["leisure"]?.let { OSM_OTHER_TO_ICON[it] }?.let { return it }
        tags["amenity"]?.let { OSM_OTHER_TO_ICON[it] }?.let { return it }
        tags["tourism"]?.let { OSM_OTHER_TO_ICON[it] }?.let { return it }
        tags["man_made"]?.let { OSM_OTHER_TO_ICON[it] }?.let { return it }
        // Additional OSM keys
        tags["shop"]?.let { v ->
            when (v) {
                "supermarket", "convenience", "grocery" -> return "shopping_cart"
                "clothes", "shoes", "jewelry"           -> return "storefront"
                "bakery"                                -> return "bakery_dining"
                "coffee"                                -> return "local_cafe"
                "alcohol", "wine"                       -> return "wine_bar"
                "car", "car_repair"                     -> return "directions_car"
                "bicycle"                               -> return "directions_bike"
                "fuel"                                  -> return "local_gas_station"
                else                                    -> return "storefront"
            }
        }
        tags["railway"]?.let { v ->
            when (v) {
                "station", "halt", "stop" -> return "train"
                "tram_stop"               -> return "tram"
                else                      -> null
            }
        }
        tags["aeroway"]?.let { v ->
            when (v) {
                "aerodrome", "terminal" -> return "flight"
                else                    -> null
            }
        }
        return "place"
    }

    /**
     * Resolves an icon key from a freeform imported GPX <type> string.
     * Case-insensitive substring match against the keyword table. First match wins.
     * Returns "place" as fallback.
     */
    fun resolveForImportedType(typeStr: String): String {
        if (typeStr.isBlank()) return "place"
        val lower = typeStr.lowercase()
        for ((keywords, iconKey) in IMPORT_KEYWORD_TABLE) {
            if (keywords.any { lower.contains(it) }) return iconKey
        }
        return "place"
    }

    /**
     * Resolves an icon key from the waypoint's name and/or description.
     * Used as a fallback when no <type> element is present (e.g. AmudAnan-style GPX).
     * Checks Hebrew keywords first, then falls back to the English table.
     * Returns "place" as fallback.
     */
    fun resolveForImportedName(name: String, desc: String): String {
        // Strip Unicode directional marks (U+200F RTL, U+200E LTR, U+200B ZWS)
        // that appear in some GPX exports and break substring matching.
        fun normalize(s: String) = s.replace("\u200F", "").replace("\u200E", "").replace("\u200B", "")

        // Check Hebrew table against name, then desc
        for (raw in listOf(name, desc)) {
            if (raw.isBlank()) continue
            val text = normalize(raw)
            for ((keywords, iconKey) in IMPORT_HEBREW_KEYWORD_TABLE) {
                if (keywords.any { text.contains(it) }) return iconKey
            }
        }
        // Fall back to English keyword table against name, then desc
        for (raw in listOf(name, desc)) {
            if (raw.isBlank()) continue
            val lower = normalize(raw).lowercase()
            for ((keywords, iconKey) in IMPORT_KEYWORD_TABLE) {
                if (keywords.any { lower.contains(it) }) return iconKey
            }
        }
        return "place"
    }
}
