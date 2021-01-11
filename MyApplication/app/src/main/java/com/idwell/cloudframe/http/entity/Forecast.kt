package com.idwell.cloudframe.http.entity

data class Forecast(
    var coord: Coord = Coord(),
    var country: String = "",
    var cur_data: CurData = CurData(),
    var days_data: List<DaysData> = listOf(),
    var id: String = "",
    var name: String = ""
) {
    data class DaysData(
        var max: Max = Max(),
        var min: Min = Min()
    ) {
        data class Max(
            var dt: Int = 0,
            var main: Main = Main(),
            var weather: List<Weather> = listOf(),
            var wind: Wind = Wind()
        ) {
            data class Weather(
                var description: String = "",
                var icon: String = "",
                var id: Int = 0,
                var main: String = ""
            )

            data class Wind(
                var deg: Double = 0.0,
                var speed: Double = 0.0
            )

            data class Main(
                var humidity: Int = 0,
                var temp: Double = 0.0,
                var temp_kf: Double = 0.0,
                var temp_max: Double = 0.0,
                var temp_min: Double = 0.0
            )
        }

        data class Min(
            var dt: Int = 0,
            var main: Main = Main(),
            var weather: List<Weather> = listOf(),
            var wind: Wind = Wind()
        ) {
            data class Weather(
                var description: String = "",
                var icon: String = "",
                var id: Int = 0,
                var main: String = ""
            )

            data class Wind(
                var deg: Double = 0.0,
                var speed: Double = 0.0
            )

            data class Main(
                var humidity: Int = 0,
                var temp: Double = 0.0,
                var temp_kf: Double = 0.0,
                var temp_max: Double = 0.0,
                var temp_min: Double = 0.0
            )
        }
    }

    data class CurData(
        var dt: Int = 0,
        var main: Main = Main(),
        var weather: List<Weather> = listOf(),
        var wind: Wind = Wind()
    ) {
        data class Weather(
            var description: String = "",
            var icon: String = "",
            var id: Int = 0,
            var main: String = ""
        )

        data class Wind(
            var deg: Double = 0.0,
            var speed: Double = 0.0
        )

        data class Main(
            var humidity: Int = 0,
            var temp: Double = 0.0,
            var temp_kf: Double = 0.0,
            var temp_max: Double = 0.0,
            var temp_min: Double = 0.0
        )
    }

    data class Coord(
        var lat: Double = 0.0,
        var lon: Double = 0.0
    )
}