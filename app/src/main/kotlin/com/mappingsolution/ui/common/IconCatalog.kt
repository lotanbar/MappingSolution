package com.mappingsolution.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconCatalog {

    data class IconEntry(val key: String, val vector: ImageVector, val label: String)
    data class IconCategory(val name: String, val icons: List<IconEntry>)

    val categories: List<IconCategory> = listOf(
        IconCategory(
            "Location", listOf(
                IconEntry("place", Icons.Default.Place, "Place"),
                IconEntry("location_on", Icons.Default.LocationOn, "Location"),
                IconEntry("my_location", Icons.Default.MyLocation, "My Location"),
                IconEntry("explore", Icons.Default.Explore, "Explore"),
                IconEntry("travel_explore", Icons.Default.TravelExplore, "Travel"),
                IconEntry("navigation", Icons.Default.Navigation, "Navigation"),
                IconEntry("near_me", Icons.Default.NearMe, "Near Me"),
                IconEntry("gps_fixed", Icons.Default.GpsFixed, "GPS"),
                IconEntry("flag", Icons.Default.Flag, "Flag"),
                IconEntry("tour", Icons.Default.Tour, "Tour"),
                IconEntry("map", Icons.Default.Map, "Map"),
                IconEntry("push_pin", Icons.Default.PushPin, "Pin"),
                IconEntry("satellite", Icons.Default.Satellite, "Satellite"),
                IconEntry("location_city", Icons.Default.LocationCity, "City"),
            )
        ),
        IconCategory(
            "Nature", listOf(
                IconEntry("park", Icons.Default.Park, "Park"),
                IconEntry("terrain", Icons.Default.Terrain, "Terrain"),
                IconEntry("waves", Icons.Default.Waves, "Waves"),
                IconEntry("water_drop", Icons.Default.WaterDrop, "Water"),
                IconEntry("landscape", Icons.Default.Landscape, "Landscape"),
                IconEntry("nature", Icons.Default.Nature, "Nature"),
                IconEntry("grass", Icons.Default.Grass, "Grass"),
                IconEntry("forest", Icons.Default.Forest, "Forest"),
                IconEntry("spa", Icons.Default.Spa, "Spa"),
                IconEntry("filter_vintage", Icons.Default.FilterVintage, "Flower"),
                IconEntry("eco", Icons.Default.Eco, "Eco"),
                IconEntry("ac_unit", Icons.Default.AcUnit, "Snow"),
                IconEntry("wb_sunny", Icons.Default.WbSunny, "Sunny"),
                IconEntry("cloud", Icons.Default.Cloud, "Cloud"),
            )
        ),
        IconCategory(
            "Food & Drink", listOf(
                IconEntry("restaurant", Icons.Default.Restaurant, "Restaurant"),
                IconEntry("local_cafe", Icons.Default.LocalCafe, "Cafe"),
                IconEntry("local_bar", Icons.Default.LocalBar, "Bar"),
                IconEntry("fastfood", Icons.Default.Fastfood, "Fast Food"),
                IconEntry("lunch_dining", Icons.Default.LunchDining, "Lunch"),
                IconEntry("dinner_dining", Icons.Default.DinnerDining, "Dinner"),
                IconEntry("brunch_dining", Icons.Default.BrunchDining, "Brunch"),
                IconEntry("bakery_dining", Icons.Default.BakeryDining, "Bakery"),
                IconEntry("ramen_dining", Icons.Default.RamenDining, "Ramen"),
                IconEntry("local_pizza", Icons.Default.LocalPizza, "Pizza"),
                IconEntry("icecream", Icons.Default.Icecream, "Ice Cream"),
                IconEntry("cake", Icons.Default.Cake, "Cake"),
                IconEntry("wine_bar", Icons.Default.WineBar, "Wine"),
                IconEntry("coffee", Icons.Default.Coffee, "Coffee"),
            )
        ),
        IconCategory(
            "Activities", listOf(
                IconEntry("directions_walk", Icons.AutoMirrored.Filled.DirectionsWalk, "Walking"),
                IconEntry("directions_run", Icons.AutoMirrored.Filled.DirectionsRun, "Running"),
                IconEntry("directions_bike", Icons.AutoMirrored.Filled.DirectionsBike, "Cycling"),
                IconEntry("hiking", Icons.Default.Hiking, "Hiking"),
                IconEntry("fitness_center", Icons.Default.FitnessCenter, "Gym"),
                IconEntry("pool", Icons.Default.Pool, "Swimming"),
                IconEntry("sailing", Icons.Default.Sailing, "Sailing"),
                IconEntry("kayaking", Icons.Default.Kayaking, "Kayaking"),
                IconEntry("snowboarding", Icons.Default.Snowboarding, "Snowboarding"),
                IconEntry("downhill_skiing", Icons.Default.DownhillSkiing, "Skiing"),
                IconEntry("surfing", Icons.Default.Surfing, "Surfing"),
                IconEntry("sports_soccer", Icons.Default.SportsSoccer, "Soccer"),
                IconEntry("sports_basketball", Icons.Default.SportsBasketball, "Basketball"),
                IconEntry("golf_course", Icons.Default.GolfCourse, "Golf"),
                IconEntry("paragliding", Icons.Default.Paragliding, "Paragliding"),
            )
        ),
        IconCategory(
            "Accommodation", listOf(
                IconEntry("hotel", Icons.Default.Hotel, "Hotel"),
                IconEntry("home", Icons.Default.Home, "Home"),
                IconEntry("apartment", Icons.Default.Apartment, "Apartment"),
                IconEntry("house", Icons.Default.House, "House"),
                IconEntry("night_shelter", Icons.Default.NightShelter, "Shelter"),
                IconEntry("beach_access", Icons.Default.BeachAccess, "Beach"),
                IconEntry("king_bed", Icons.Default.KingBed, "Bed"),
                IconEntry("single_bed", Icons.Default.SingleBed, "Single Bed"),
                IconEntry("meeting_room", Icons.Default.MeetingRoom, "Room"),
            )
        ),
        IconCategory(
            "Transport", listOf(
                IconEntry("directions_car", Icons.Default.DirectionsCar, "Car"),
                IconEntry("directions_bus", Icons.Default.DirectionsBus, "Bus"),
                IconEntry("train", Icons.Default.Train, "Train"),
                IconEntry("flight", Icons.Default.Flight, "Flight"),
                IconEntry("motorcycle", Icons.Default.Motorcycle, "Motorcycle"),
                IconEntry("two_wheeler", Icons.Default.TwoWheeler, "Two Wheeler"),
                IconEntry("electric_car", Icons.Default.ElectricCar, "Electric Car"),
                IconEntry("directions_boat", Icons.Default.DirectionsBoat, "Boat"),
                IconEntry("anchor", Icons.Default.Anchor, "Anchor"),
                IconEntry("local_taxi", Icons.Default.LocalTaxi, "Taxi"),
                IconEntry("tram", Icons.Default.Tram, "Tram"),
            )
        ),
        IconCategory(
            "Services", listOf(
                IconEntry("local_hospital", Icons.Default.LocalHospital, "Hospital"),
                IconEntry("local_pharmacy", Icons.Default.LocalPharmacy, "Pharmacy"),
                IconEntry("local_gas_station", Icons.Default.LocalGasStation, "Gas Station"),
                IconEntry("local_parking", Icons.Default.LocalParking, "Parking"),
                IconEntry("shopping_cart", Icons.Default.ShoppingCart, "Shopping"),
                IconEntry("storefront", Icons.Default.Storefront, "Store"),
                IconEntry("local_atm", Icons.Default.LocalAtm, "ATM"),
                IconEntry("account_balance", Icons.Default.AccountBalance, "Bank"),
                IconEntry("school", Icons.Default.School, "School"),
                IconEntry("local_police", Icons.Default.LocalPolice, "Police"),
                IconEntry("local_fire_department", Icons.Default.LocalFireDepartment, "Fire Dept"),
                IconEntry("local_laundry", Icons.Default.LocalLaundryService, "Laundry"),
            )
        ),
        IconCategory(
            "Entertainment", listOf(
                IconEntry("museum", Icons.Default.Museum, "Museum"),
                IconEntry("music_note", Icons.Default.MusicNote, "Music"),
                IconEntry("nightlife", Icons.Default.Nightlife, "Nightlife"),
                IconEntry("theaters", Icons.Default.Theaters, "Theater"),
                IconEntry("casino", Icons.Default.Casino, "Casino"),
                IconEntry("sports_bar", Icons.Default.SportsBar, "Sports Bar"),
                IconEntry("sports_esports", Icons.Default.SportsEsports, "Gaming"),
                IconEntry("photo_camera", Icons.Default.PhotoCamera, "Camera"),
                IconEntry("attractions", Icons.Default.Attractions, "Attractions"),
            )
        ),
        IconCategory(
            "Markers", listOf(
                IconEntry("star", Icons.Default.Star, "Star"),
                IconEntry("favorite", Icons.Default.Favorite, "Favorite"),
                IconEntry("bookmark", Icons.Default.Bookmark, "Bookmark"),
                IconEntry("label", Icons.AutoMirrored.Filled.Label, "Label"),
                IconEntry("warning", Icons.Default.Warning, "Warning"),
                IconEntry("info", Icons.Default.Info, "Info"),
                IconEntry("emergency", Icons.Default.Emergency, "Emergency"),
                IconEntry("whatshot", Icons.Default.Whatshot, "Hot"),
                IconEntry("bolt", Icons.Default.Bolt, "Bolt"),
                IconEntry("visibility", Icons.Default.Visibility, "Visible"),
                IconEntry("work", Icons.Default.Work, "Work"),
                IconEntry("business_center", Icons.Default.BusinessCenter, "Business"),
            )
        ),
    )

    private val allIcons: Map<String, ImageVector> by lazy {
        categories.flatMap { it.icons }.associate { it.key to it.vector }
    }

    fun iconVector(key: String): ImageVector = allIcons[key] ?: Icons.Default.Place
}
