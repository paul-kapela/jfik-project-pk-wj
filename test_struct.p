struct Rectangle
    x double
    y double
    width real
    height real
endstruct

r = Rectangle
r.x = 10.05
r.y = 30.20
r.width = 52.40f
r.height = 32.23f

write "Pole"
write r.width * r.height

write "Koniec"
write "X:"
write r.x + r.width
write "Y:"
write r.y + r.height
