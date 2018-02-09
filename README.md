# DynaTest Dynamic Testing Framework piggybacking on JUnit5 (experimental)

Experimental, unreleased, pretty much a playground. Code example:

```kotlin
class Calculator {
    fun plusOne(i: Int) = i + 1
    fun close() {}
}

class CalculatorTest : DynaTest({
    calculatorBattery(0..10)
    calculatorBattery(-50..-40)
    calculatorBattery(90..100)
})

fun DynaNodeGroup.calculatorBattery(range: IntRange) {
    kotlin.require(!range.isEmpty())

    group("plusOne on $range") {
        lateinit var c: Calculator

        beforeEach { c = Calculator() }
        afterEach { c.close() }

        for (i in range) {
            test("plusOne($i) == ${i + 1}") {
                expect(i + 1) { c.plusOne(i) }
            }
        }
    }
}
```

