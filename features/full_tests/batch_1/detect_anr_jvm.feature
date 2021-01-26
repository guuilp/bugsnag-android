Feature: ANRs triggered in JVM code are captured

@skip_android_8_1
Scenario: ANR triggered in JVM loop code is captured
    When I run "JvmAnrLoopScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the error "Bugsnag-Stacktrace-Types" header equals "android"
    And the error payload field "events.0.exceptions.0.type" equals "android"
    And the error payload field "events.0.exceptions.0.stacktrace.0.type" is null
    And the error payload field "events.0.threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace.0.type" is null

@skip_android_8_1
Scenario: ANR triggered in JVM sleep code is captured
    When I run "JvmAnrSleepScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the error "Bugsnag-Stacktrace-Types" header equals "android"
    And the error payload field "events.0.exceptions.0.type" equals "android"
    And the error payload field "events.0.exceptions.0.stacktrace.0.type" is null
    And the error payload field "events.0.threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace.0.type" is null

@skip_android_8_1
Scenario: ANR triggered in JVM code is not captured when detectAnrs = false
    When I run "JvmAnrDisabledScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "JvmAnrDisabledScenario"

@skip_android_8_1
Scenario: ANR triggered in JVM code is not captured when outside of release stage
    When I run "JvmAnrOutsideReleaseStagesScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I should receive no requests
