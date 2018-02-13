# DynaTest Dynamic Testing

[![Build Status](https://travis-ci.org/mvysny/dynatest.svg?branch=master)](https://travis-ci.org/mvysny/dynatest)

Traditional JUnit/TestNG approach is to have a bunch of test classes with `@Test`-annotated methods. That's not bad per se,
but it would seem as if the ultimate JUnit's goal was that the test collection must be *pre-known* - computable by static-analyzing class files alone,
without running any test generator code whatsoever. With such approach, 
the possibilities to create tests dynamically (e.g. creating a reusable test suite) are severely limited. I believe this requirement is not only
useless in modern programming,
it also *reduces the possibilities* of how to structure test code and *promotes bad practices*:

* You simply can't create a parametrized test suite class as a component, instantiating and registering
  it dynamically as required
* There is a possibility to have parametrized tests with JUnit5, but it's quite limited: the parameters need to be known upfront and stored
  in annotations, that restricts you from:
  * Having the parameters created dynamically or loaded from a file;
  * Using more complex types - you are restricted to String and primitives.
* Annotations are weak - they don't have the full power of a programming language. Overusing annotations in stead of 
  an actual programming language leads to horrible constructs and annotationmania.
* Reuse of test suites is only possible by the means of inheritance (having a base class with tests, and a bunch of classes
  extending that base class, parametrizing it with constructors). Typically componentization should be preferred over inheritance.
* Even worse, it is possible to "reuse" test suites by the means of interface mixins. That's a whole new level
  of inheritance hell.

Think of Maven (with `pom.xml` with no computative power) versus Gradle - a build script with tasks
generated by a full-blown programming language. We need to throw away the old ways of configuration-using-static declarations,
and embrace testing structures created programmatically.

## Disadvantages

It's not just unicorns:

* There is no clear distinction between the code that *creates* tests (calls the `test()` method), and
  the *testing* code itself (blocks inside of `test()` method). However, there is a ton of difference:
  those two code bases run at completely different time. Furthermore Kotlin allows to share variables
  freely between those two code bases, which may create a really dangerous code which fails in mysterious ways.
  That's magic which must be removed. See [Issue #1](https://github.com/mvysny/dynatest/issues/1) for more details.

## Design principles

Ideally, the testing framework should follow these items:

* Promote component-oriented programming. You should be able to create a test suite as a component,
  and simply include that test suite anywhere you see fit.
* Dissuade from abominable programming techniques like inheritance and annotationmania.
* Allow creating tests dynamically, in a fucking `for` loop if necessary.
* Put the programmer in charge and allow him to use the full palette of software practices, in order
  to create well-maintainable test code.
* With great power comes great responsibility. Don't make the test structure generation code more complex
  than anything else in your project. Keep it simple.

What this framework is not:

* BDD. What BDD strives for is to describe a behavior of an app. What it really does is that it provides
  a really lousy, obfuscated and computationally weak way of writing test programs. There is no good. For bad, head to
  [JBehave](http://jbehave.org/). To experience a real horror, head to [Robot Framework](http://robotframework.org/).
* Spec. What spec strives for is to describe a specification of an app. What it really does is that it provides
  a lousy way of writing test programs. If you want spec, use [Spek](http://spekframework.org/).

## Example

Code example of the [CalculatorTest.kt](src/test/kotlin/CalculatorTest.kt):

```kotlin
class Calculator {
    fun plusOne(i: Int) = i + 1
    fun close() {}
}

/**
 * A test case.
 */
class CalculatorTest : DynaTest({

    /**
     * Top-level test.
     */
    test("calculator instantiation test") {
        Calculator()
    }

    // you can have as many groups as you like, and you can nest them
    group("tests the plusOne() function") {

        // demo of the very simple test
        test("one plusOne") {
            expect(2) { Calculator().plusOne(1) }
        }

        // nested group
        group("positive numbers") {
            // you can even create a reusable test battery, call it from anywhere and use any parameters you like.
            calculatorBattery(0..10)
            calculatorBattery(100..110)
        }

        group("negative numbers") {
            calculatorBattery(-50..-40)
        }
    }
})

/**
 * Demonstrates a reusable test battery which can be called repeatedly and parametrized.
 * @receiver all tests+groups do not run immediately, but instead they register themselves to this group; they are run later on
 * when launched by JUnit5
 * @param range parametrized battery demo
 */
fun DynaNodeGroup.calculatorBattery(range: IntRange) {
    require(!range.isEmpty())

    group("plusOne on $range") {
        lateinit var c: Calculator

        // analogous to @Before in JUnit4, or @BeforeEach in JUnit5
        beforeEach { c = Calculator() }
        // analogous to @After in JUnit4, or @AfterEach in JUnit5
        afterEach { c.close() }

        // we can even generate test cases in a loop
        for (i in range) {
            test("plusOne($i) == ${i + 1}") {
                expect(i + 1) { c.plusOne(i) }
            }
        }
    }
}
```

Running this in your IDE will produce:

![DynaTest CalculatorTest screenshot](images/dynatest.png)

## Current drawbacks

* Weak IDE (Intellij) integration:
  * "Rerun failed tests" always runs all tests
  * Impossible to run single test only (right-clicking on the test name doesn't offer such option)

## Using in your projects

DynaTest sports its own TestEngine which ignores JUnit5 tests and only runs `DynaTest` tests.
If you don't have any JUnit5 tests in your project, you only need to add a test dependency on this library:

```groovy
dependencies {
    testCompile("com.github.mvysny.dynatest:dynatest:0.0.1")
}
```

Moreover you need to add the [junit5-gradle-consumer](https://github.com/junit-team/junit5-samples/tree/r5.0.3/junit5-gradle-consumer)
plugin to your build script, to actually run the tests; see the plugin's documentation for details. This is required since
Gradle doesn't have built-in support for running JUnit5 tests yet.

If you have JUnit5 tests as well, you can run both DynaTest test engine along with JUnit5 Jupiter engine
(which will only run JUnit5 tests and will ignore DynaTest tests):

```groovy
dependencies {
    testCompile("com.github.mvysny.dynatest:dynatest:0.0.1")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.0.3")
}
```
