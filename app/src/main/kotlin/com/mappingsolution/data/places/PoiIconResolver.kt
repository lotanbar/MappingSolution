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
        "cave_entrance"  to "explore",
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
        // ── Precise food sub-types (before generic restaurant) ───────────────
        listOf("sushi", "japanese restaurant", "ramen", "noodle", "dumpling", "dim sum", "udon", "pho") to "ramen_dining",
        listOf("pizza")                                                              to "local_pizza",
        listOf("ice cream", "icecream", "gelato", "frozen yogurt", "sorbet")        to "icecream",
        listOf("cake", "dessert", "pastry", "patisserie", "sweets", "confection")   to "cake",
        listOf("bakery", "boulangerie", "bread")                                    to "bakery_dining",
        listOf("wine bar", "winery", "vineyard", "cellar")                          to "wine_bar",
        listOf("coffee", "espresso", "cappuccino", "latte", "barista")              to "coffee",
        listOf("cafe", "bistro cafe", "tea house", "tea room", "tearoom")           to "local_cafe",
        listOf("bar", "pub", "tavern", "brewery", "saloon", "taproom", "alehouse")  to "local_bar",
        listOf("fast food", "fastfood", "burger", "sandwich", "kebab", "shawarma",
               "falafel", "taco", "burrito", "wrap", "hot dog", "chips")            to "fastfood",
        listOf("steakhouse", "steak", "bbq", "barbeque", "barbecue", "grill",
               "seafood", "fish restaurant", "sushi restaurant")                    to "restaurant",
        listOf("restaurant", "diner", "eatery", "bistro", "buffet", "brasserie",
               "trattoria", "osteria", "cantina", "taverna", "tapas", "mezze")      to "restaurant",
        // ── Beach / coast ────────────────────────────────────────────────────
        listOf("beach", "shore", "coast", "seaside", "coastline", "bay")            to "beach_access",
        // ── Mountain / terrain (ridge & slope fall back to terrain icon) ─────
        listOf("mountain peak", "summit", "mountain top", "mountaintop")            to "terrain",
        listOf("volcano", "volcanic")                                                to "terrain",
        listOf("mountain", "peak", "hill", "ridge", "cliff", "highland",
               "escarpment", "slope", "bluff", "butte", "mesa")                    to "terrain",
        // ── Canyon / gorge / valley (landscape icon = terrain layers) ────────
        listOf("canyon", "gorge", "ravine", "gully", "chasm")                       to "landscape",
        listOf("valley", "vale", "dale", "glen")                                    to "landscape",
        // ── Cave / cavern (explore = discovery/exploration) ──────────────────
        listOf("cave", "cavern", "grotto", "spelunking", "cave entrance")           to "explore",
        // ── Water features ───────────────────────────────────────────────────
        listOf("waterfall", "cascade", "rapids", "cataract")                        to "waves",
        listOf("river", "stream", "creek", "brook", "wadi", "torrent")             to "waves",
        listOf("lake", "pond", "lagoon", "reservoir", "tarn")                      to "pool",
        listOf("glacier", "icefield", "ice cap", "snowfield")                       to "ac_unit",
        listOf("hot spring", "thermal spring", "thermal bath", "geyser")            to "water_drop",
        listOf("spring", "well", "cistern", "water source", "fountain")             to "water_drop",
        // ── Wetland / meadow / open land ─────────────────────────────────────
        listOf("wetland", "marsh", "swamp", "bog", "fen", "mangrove")               to "nature",
        listOf("meadow", "prairie", "steppe", "savanna", "heath", "moor")           to "nature",
        // ── Viewpoint ────────────────────────────────────────────────────────
        listOf("viewpoint", "overlook", "lookout", "vista", "panorama", "scenic",
               "belvedere", "mirador", "observation deck", "observation point",
               "observation tower", "panoramic point")                              to "visibility",
        // ── Lighthouse ───────────────────────────────────────────────────────
        listOf("lighthouse", "light house")                                          to "anchor",
        // ── Historical / archaeological ───────────────────────────────────────
        listOf("castle", "fort", "fortress", "citadel", "stronghold", "rampart",
               "battlement", "bastion", "keep", "tower house")                     to "flag",
        listOf("ruin", "ruins", "ancient", "archaeological", "archaeology",
               "excavation", "dig site", "historic site", "heritage site",
               "aqueduct", "watermill", "mill", "bridge historic",
               "roman road", "ancient road", "byzantine")                           to "museum",
        listOf("monument", "obelisk", "landmark", "pillar", "column", "statue",
               "sculpture", "memorial gate", "arch", "triumphal arch")             to "tour",
        listOf("memorial", "grave", "cemetery", "tombstone", "mausoleum",
               "burial", "tomb", "catacomb", "necropolis", "cenotaph")             to "favorite",
        // ── Religious sites ───────────────────────────────────────────────────
        listOf("church", "cathedral", "chapel", "basilica",
               "mosque", "minaret",
               "temple", "synagogue", "shrine", "shrine",
               "monastery", "abbey", "convent", "priory",
               "pilgrimage", "holy site", "place of worship")                      to "bookmark",
        // ── Museum / gallery ──────────────────────────────────────────────────
        listOf("museum", "gallery", "art gallery", "exhibit", "exhibition",
               "science center", "planetarium")                                     to "museum",
        // ── Accommodation ────────────────────────────────────────────────────
        listOf("hotel", "motel", "inn", "resort", "lodge", "accommodation",
               "guesthouse", "guest house", "pension", "chalet", "cottage",
               "b&b", "bed and breakfast", "hostal", "pousada")                    to "hotel",
        listOf("hostel", "campsite", "camp site", "camp ground", "camping",
               "caravan", "glamping", "tent", "bivouac", "hut", "alpine hut",
               "wilderness hut", "refuge", "bothy")                                to "night_shelter",
        listOf("home", "house", "residence", "villa", "farmhouse")                 to "home",
        listOf("apartment", "flat", "condominium")                                  to "apartment",
        // ── Health / Medical ──────────────────────────────────────────────────
        listOf("hospital", "clinic", "medical center", "health center",
               "urgent care", "emergency room")                                     to "local_hospital",
        listOf("pharmacy", "drugstore", "chemist", "apothecary")                   to "local_pharmacy",
        // ── Education ────────────────────────────────────────────────────────
        listOf("school", "university", "college", "academy", "campus",
               "kindergarten", "high school", "elementary school")                 to "school",
        // ── Nature / parks ───────────────────────────────────────────────────
        listOf("national park", "nature reserve", "wildlife reserve",
               "nature sanctuary", "wildlife sanctuary", "habitat",
               "nature area", "ecological")                                         to "nature",
        listOf("forest", "woods", "woodland", "jungle", "rainforest", "grove")     to "forest",
        listOf("park", "city park", "garden", "botanical garden", "playground",
               "recreation area", "picnic")                                         to "park",
        // ── Observatory ──────────────────────────────────────────────────────
        listOf("observatory", "telescope", "astronomy")                             to "satellite",
        // ── Services ─────────────────────────────────────────────────────────
        listOf("gas station", "petrol station", "fuel station", "gas", "fuel",
               "petrol", "diesel", "refuel", "filling station")                    to "local_gas_station",
        listOf("parking", "car park", "parking lot", "garage parking")             to "local_parking",
        listOf("atm", "cash machine", "cash point")                                to "local_atm",
        listOf("bank", "finance", "credit union", "savings bank")                  to "account_balance",
        listOf("supermarket", "grocery", "market", "mall", "shopping center",
               "shop", "store", "boutique", "shopping")                            to "shopping_cart",
        listOf("laundry", "dry clean", "dry cleaner", "laundromat")                to "local_laundry",
        // ── Transport ────────────────────────────────────────────────────────
        listOf("airport", "terminal", "aviation", "airfield", "airstrip")          to "flight",
        listOf("train station", "railway station", "rail station",
               "train", "railway", "rail", "metro station", "subway station")      to "train",
        listOf("bus station", "bus stop", "bus terminal", "transit hub",
               "bus", "transit")                                                    to "directions_bus",
        listOf("taxi", "cab", "rideshare")                                          to "local_taxi",
        listOf("marina", "port", "harbor", "harbour", "dock", "pier",
               "jetty", "wharf", "ferry terminal", "boat dock")                    to "anchor",
        // ── Activities / sport ────────────────────────────────────────────────
        listOf("skiing", "ski resort", "ski slope", "piste", "ski run",
               "ski lift", "chairlift", "snowboard")                               to "downhill_skiing",
        listOf("golf course", "golf club", "golf")                                  to "golf_course",
        listOf("swimming", "swimming pool", "aquatic center", "lido")               to "pool",
        listOf("surfing", "surf spot", "surf break")                                to "surfing",
        listOf("kayak", "kayaking", "canoe", "canoeing", "rafting",
               "white water", "whitewater")                                         to "kayaking",
        listOf("sailing", "yacht", "yachting", "regatta")                          to "sailing",
        listOf("paragliding", "hang gliding", "gliding", "paraglider launch")      to "paragliding",
        listOf("gym", "fitness", "fitness center", "workout", "crossfit",
               "yoga", "pilates", "aerobics", "spin", "weightlifting")             to "fitness_center",
        listOf("climbing", "bouldering", "rock climbing", "via ferrata",
               "climbing wall", "crag")                                             to "hiking",
        listOf("tennis", "volleyball", "basketball court", "football pitch",
               "baseball", "rugby", "cricket", "badminton", "squash")             to "sports_soccer",
        listOf("stadium", "arena", "sports complex", "sports center",
               "sports hall")                                                       to "sports_soccer",
        listOf("fishing", "angling", "fish")                                        to "sailing",
        // ── Trail navigation ──────────────────────────────────────────────────
        listOf("trailhead", "trail start", "trail end", "trail head",
               "start point", "end point", "navigation point")                     to "navigation",
        listOf("trail junction", "trail fork", "trail split", "intersection",
               "crossroads", "junction")                                            to "near_me",
        listOf("hiking", "trail", "trekking", "trek", "walk path",
               "footpath", "walking route", "long distance trail",
               "waymark", "waymarked")                                              to "hiking",
        listOf("cycling route", "bike path", "bicycle route",
               "cycling", "bicycle", "bike")                                       to "directions_bike",
        // ── Emergency services ────────────────────────────────────────────────
        listOf("fire station", "fire department", "firehouse")                      to "local_fire_department",
        listOf("police", "police station", "law enforcement", "security post")      to "local_police",
        // ── Entertainment ─────────────────────────────────────────────────────
        listOf("concert", "concert hall", "music venue", "music")                  to "music_note",
        listOf("theater", "theatre", "cinema", "movie", "movie theater",
               "film", "opera house", "opera", "playhouse")                        to "theaters",
        listOf("nightclub", "night club", "club", "disco", "lounge",
               "cocktail bar")                                                      to "nightlife",
        listOf("casino", "gambling")                                                to "casino",
        // ── Building / office ─────────────────────────────────────────────────
        listOf("building", "office", "office building")                             to "apartment",
    )

    // ── Imported GPX fuzzy keyword table — Hebrew ────────────────────────────
    // Covers common hiking/trail POI terminology in Israeli GPX exports.
    // Words are matched as substrings (case doesn't apply to Hebrew).

    private val IMPORT_HEBREW_KEYWORD_TABLE: List<Pair<List<String>, String>> = listOf(
        // Burial caves — must be before generic cave/water so they win
        listOf("מערת קבורה", "מערת קבר", "מערות קבורה", "מערת קבורות") to "favorite",

        // Terrain / topography
        // NOTE: standalone "ראש" EXCLUDED — it is too generic (appears in "ראש העין" etc.)
        // NOTE: standalone "גב" EXCLUDED — too generic (suffix/prefix in many unrelated words)
        // NOTE: "מעלה"/"מורד" moved to hiking (trail ascent/descent markers, not mountain tops)
        listOf("פסגה", "מצוק", "גבעה", "ראש ההר", "ראש הר", "רכס", "גב הר", "כיפה") to "terrain",
        listOf("מעבר", "צוואר", "אוכף") to "navigation",

        // Canyon / gorge / valley
        listOf("עמק", "גיא", "קניון", "ערוץ", "תהום") to "landscape",

        // Water features
        listOf("מפל", "מפלים", "מפלון") to "waves",
        listOf("נחל") to "waves",
        listOf("ואדי") to "waves",
        listOf("סכר") to "waves",  // dam / weir
        // NOTE: "ים" alone EXCLUDED — it is the Hebrew plural suffix (שרידים, קברים, etc.)
        listOf("חוף ים", "חוף הים", "חוף", "ים המלח", "כנרת", "ים סוף") to "beach_access",
        listOf("מעיין", "מעין", "עין", "גבים", "נביעה",
               "בור מים", "בריכת מים", "בריכת", "בורות מים", "בורות",
               "בור", "באר", "באר מים", "מאגר מים", "ברז מים", "ציר מים",
               "מגדל מים", "מגדל המים", "מעביר מים", "מעבר מים") to "water_drop",
        listOf("אגם", "ברכה", "בריכה", "בריכת חורף", "שלולית") to "pool",
        listOf("ביצה", "אזור לח") to "nature",

        // Caves — after burial cave entry; now mapped to explore (not landscape)
        listOf("מערה", "מחילה", "נקיק", "מערות", "מערת", "מערונת", "פתח מערה") to "explore",

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
               "מחסום", "סכנת", "דרך חסומה", "דרך משובשת",
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

        // Trails / navigation — "מעלה"/"מורד" moved here (trail ascent/descent, not mountain tops)
        listOf("נקודת התחלה", "נקודת סיום", "ניווט", "נקודה") to "navigation",
        listOf("פיצול שבילים", "צומת שבילים", "מפגש שבילים", "מפגש",
               "פיצול", "צומת", "מסעף") to "near_me",
        listOf("סימן דרך", "סימן ") to "hiking",  // trail marker/sign
        listOf("שביל", "מסלול", "תוואי", "נקודת חובה", "מעלה", "מורד") to "hiking",

        // Parks / recreation areas
        listOf("פיקניק", "שולחן פיקניק", "שולחנות", "אזור פיקניק", "פינת ישיבה", "בוסתן") to "park",
        listOf("גן", "פארק", "גינה") to "park",

        // Food & drink
        listOf("מסעדה") to "restaurant",
        listOf("קפה", "בית קפה") to "local_cafe",
        listOf("מאפייה", "פלאפל", "שווארמה", "המבורגר") to "fastfood",

        // Services
        listOf("תחנת דלק", "תדלוק") to "local_gas_station",
        listOf("חניה", "חניון") to "local_parking",
        listOf("בית חולים", "קופת חולים", "מרפאה") to "local_hospital",
        listOf("בית ספר", "אוניברסיטה", "מכללה") to "school",
        listOf("בנק", "כספומט") to "account_balance",
        listOf("מרכול", "סופרמרקט", "קניון", "חנות") to "shopping_cart",

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
