@login
Feature: Login
  As a user of the SauceDemo store
  I want to log in with my credentials
  So that I can access the products page

  Background:
    Given the user is on the login page

  @smoke @positive
  Scenario: Successful login with valid credentials
    When the user logs in with valid credentials
    Then the inventory page is displayed
    And the products are listed

  @negative
  Scenario: Locked out user cannot log in
    When the user logs in with username "locked_out_user" and password "secret_sauce"
    Then a login error is shown
    And the error message contains "locked out"
    And the inventory page is not displayed

  @negative
  Scenario Outline: Invalid credentials are rejected
    When the user logs in with username "<username>" and password "<password>"
    Then a login error is shown
    And the error message contains "<message>"

    Examples:
      | username      | password      | message                                    |
      | wrong_user    | secret_sauce  | Username and password do not match         |
      | standard_user | wrong_pass    | Username and password do not match         |
      |               | secret_sauce  | Username is required                       |
      | standard_user |               | Password is required                       |
