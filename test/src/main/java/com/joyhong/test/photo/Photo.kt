package com.joyhong.test.photo

class Photo: Comparable<Photo> {
    var id: Long = 0
    var data = ""
    var size: Long = 0
    var display_name = ""
    var title = ""
    var date_modified = 0L
    var mime_type = ""
    var width = 0
    var height = 0
    var description = ""
    var datetaken = 0L
    var orientation = 0
    var isSelected = false

    constructor()

    constructor(id: Long) {
        this.id = id
    }

    constructor(data: String) {
        this.data = data
    }

    constructor(id: Long, data: String, size: Long, displayName: String, title: String, date_modified: Long, mime_type: String, width: Int, height: Int, description: String, datetaken: Long, orientation: Int) {
        this.id = id
        this.data = data
        this.size = size
        this.display_name = displayName
        this.title = title
        this.date_modified = date_modified
        this.mime_type = mime_type
        this.width = width
        this.height = height
        this.description = description
        this.datetaken = datetaken
        this.orientation = orientation
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Photo

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun compareTo(other: Photo): Int {
        if (this.datetaken > other.datetaken) {
            return -1
        }else if (this.datetaken < other.datetaken) {
            return 1
        }else {
            return 0
        }
    }

    override fun toString(): String {
        return "Photo(id=$id, data='$data', size=$size, display_name='$display_name', title='$title', date_modified=$date_modified, mime_type='$mime_type', width=$width, height=$height, description='$description', datetaken=$datetaken, orientation=$orientation, isSelected=$isSelected)"
    }

}