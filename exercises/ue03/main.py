import pandas as pd

df = pd.read_csv('subjectinfo.csv')
dfMeasure = pd.read_csv('testmeasure.csv')

age = df['Age']
mass = df['Weight']
gender = df['Sex']

vo2Max = dfMeasure['VO2'].max() / df['Weight']
vo2Rest = dfMeasure['VO2'] / df['Weight']
meanHR = dfMeasure['HR'].mean()
restHR = dfMeasure[dfMeasure['HR'] > 40]['HR'].min()
maxHR = dfMeasure['HR'].max()

slope = (vo2Max - vo2Rest) / (maxHR - restHR)
vo2 = (meanHR - restHR) * slope + vo2Rest

df['EEPettitt'] = round(vo2 * mass / 1000 * 4.8, 4)
df['EEKeytel'] = round((-59.3954 + gender * (-36.3781 + 0.271 * age + 0.394 * mass + 0.404 * vo2Max + 0.634 * meanHR)
                        + (1 - gender) * (0.274 * age + mass + 0.38 * vo2Max + 0.45 * meanHR)) / 4.184, 4)

print(df.head())
print(df.describe())

df.to_csv('energyExposure.csv')
