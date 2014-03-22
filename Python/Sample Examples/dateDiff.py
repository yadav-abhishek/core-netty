# Define a daysBetweenDates procedure that would produce the
# correct output if there was a correct nextDay procedure.
#
# Note that this will NOT produce correct ouptuts yet, since
# our nextDay procedure assumes all months have 30 days
# (hence a year is 360 days, instead of 365).
#
def isLeapYear(year) :
    if year % 4 == 0 :
        return True
    if year % 100 == 0 :
        return False
    if year % 400 == 0 :
        return True
    else :
        return False
        
    
def daysInMonth(year,month) :
    leapYear= isLeapYear(year)
    print leapYear
    if ( month == 1 or month == 3 or month ==5 or month ==7 or month ==8 or month ==10 or month ==12 ) :
        return 31
    if ( month == 4 or month == 6 or month ==9 or month == 11) :
        return 30
    if (leapYear == True) :
        if (month == 2) :
            return 29
    else :
            return 28

def nextDay(year, month, day , x):
    
    """Simple version: assume every month has 30 days"""
    monthDate = x
    
    if month == 12:
        return year + 1, 1, 1
    if month < 12 and day == monthDate :
        return year,month + 1, 1
        
    else :
        return year, month, day + 1

     
        
def daysBetweenDates(year1, month1, day1, year2, month2, day2):
    
    # YOUR CODE HERE!
    tempYear=year1
    tempMonth=month1
    tempDay=day1
    days = 0
    
    while (tempYear <= year2) :
        while tempMonth <= month2 :
            x = daysInMonth(tempYear,tempMonth)
            while tempDay <= x  :
                tempYear,tempMonth,tempDay =  nextDay(tempYear , tempMonth, tempDay, x)
                days = days + 1
            #month1= month1 + 1        
        #year1 = year1 + 1    
    print days
    return days
    

def test():
    test_cases = [((2012,9,30,2012,10,30),30), 
                  ((2012,1,1,2013,1,1),360),
                  ((2012,9,1,2012,9,4),3)]
    
    for (args, answer) in test_cases:
        result = daysBetweenDates(*args)
        if result != answer:
            print "Test with data:", args, "failed"
        else:
            print "Test case passed!"

test()

    

