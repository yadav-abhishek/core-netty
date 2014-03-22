#coins_short.py

n= int(input('Nickels? '))
d= int(input('Dimes? '))
q= int(input('Quarters? '))
#Calculate the total amount of money
total = 5*n+10*d+25*q
#print results
print() #will print a blank line
print(str(total)+ 'cents')

