Feature: BellyFeature

  Scenario: eaten many cukes
    Given I have eaten 42 cukes
    When I wait 2 hour
    Then my Belly should growl

  Scenario: not enough cukes at the boundary
    Given I have eaten 10 cukes
    When I wait 2 hour
    Then my belly should not growl

  Scenario: not enough waiting at the boundary
    Given I have eaten 11 cukes
    When I wait 1 hour
    Then my belly should not growl

  Scenario: enough cukes and enough waiting just past the boundary
    Given I have eaten 11 cukes
    When I wait 2 hour
    Then my belly should growl

  Scenario: eating a lot without waiting should not growl
    Given I have eaten 42 cukes
    Then my belly should not growl

  Scenario: waiting without enough cucumbers should not growl
    Given I have eaten 0 cukes
    When I wait 2 hour
    Then my belly should not growl

  Scenario: cucumbers from multiple snacks add up
    Given I have eaten 5 cukes
    Given I have eaten 6 cukes
    When I wait 2 hour
    Then my belly should growl

  Scenario: waiting over multiple hours adds up
    Given I have eaten 11 cukes
    When I wait 1 hour
    When I wait 1 hour
    Then my belly should growl
