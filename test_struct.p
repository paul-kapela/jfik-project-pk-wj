struct Rectangle
    x double
    y double
    width real
    height real

    fun real area()
    {
        a = this.width * this.height
        return a
    }

    fun double area100()
    {
        this.width = this.width * 100.0f
        this.height = this.height * 100.0f
        return this.width * this.height
    }
endstruct

r = Rectangle
r.x = 10.05
r.y = 30.20
r.width = 52.40f
r.height = 32.23f

write "Pole"
write r.area()
write r.width * r.height

write "area100"
write r.area100()

write "Koniec"
write "X:"
write r.x + r.width
write "Y:"
write r.y + r.height
