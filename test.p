x = 21
y = 37
read x
write y
write x * y

x = 5
write x
read x
write x

write "INT"
a = 5
b = 3
c = a + b
write "a + b"
write c
write "a - b"
write a - b
write "a * b"
write a * b
write "a / b"
write a / b
write "(10 + 5) * 3 / 5 - 2"
write (10 + 5) * 3 / 5 - 2

write "REAL"
write 3.125f
write 8.4f
d = 5.1f
e = 3.0f
f = d + e
write "a + b"
write f
write "a - b"
write d - e
write "a * b"
write d * e
write "a / b"
write d / e

write "REALD"
write 4889.16548612684684116846
write 5.0 / 3.0

text1 = "test_text1"
write text1
read text1
write text1

text256 = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis,."
write text256
write ""
text512 = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus e"
write text512

temp1 = 1000

fun int complex(int a, int b) {
    temp1 = a + b
    temp2 = temp1 * 2
    temp3 = temp2 - a

    write temp1
    write temp2

    return temp3
}

fun void readWrite() {
    write "siema"
    return
}

write complex(5, 3)

write temp2

write temp1

readWrite()
