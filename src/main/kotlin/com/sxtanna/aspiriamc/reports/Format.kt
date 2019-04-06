package com.sxtanna.aspiriamc.reports

enum class Format {

    NONE,

    SELL_ITEM, // when a player puts an item up for sell
    TAKE_ITEM, // when a player retrieves an item they were selling

    PURCHASE_COMP, // when they purchase a company
    PURCHASE_ICON, // when they purchase an icon
    PURCHASE_ITEM, // when they purchase an item
    PURCHASE_NAME, // when they rename their company

    COMPANY_DELETION, // when they close their company
    COMPANY_TRANSFER, // when they transfer the company to someone
    COMPANY_WITHDRAW, // when they withdraw money from the company

    COMPANY_STAFF_HIRE, // when they hire a staffer
    COMPANY_STAFF_FIRE, // when they fire a staffer
    COMPANY_STAFF_QUIT, // when a staffer quits the company

}