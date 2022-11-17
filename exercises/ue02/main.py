import pandas as pd
from pandas import DataFrame
from sklearn import tree
from sklearn.model_selection import train_test_split
from sklearn import metrics
import m2cgen as m2c

df = pd.read_csv('WISDM_ar_v1.1_raw.txt', lineterminator=';')


def get_peak(data: DataFrame, column=''):
    peaks = data.nlargest(n=4, columns=[column], keep='all')
    return (peaks.iloc[-1]['timestamp'] - peaks.iloc[0]['timestamp']) / len(peaks)


aggregate_df = pd.DataFrame()
step = 50
width = 200
aggregate_functions = ['mean', 'std', 'mad']

for start in range(0, len(df) - width, step):
    rollingWindow = df.iloc[start:start + width]
    user = rollingWindow.iloc[0]['user']
    activity = rollingWindow.iloc[0]['activity']
    if not ((rollingWindow['activity'] == activity).all() and (rollingWindow['user'] == user).all()):
        continue
    aggs = rollingWindow.agg({
        'x': aggregate_functions,
        'y': aggregate_functions,
        'z': aggregate_functions
    })
    mean = aggs.loc['mean']
    stds = aggs.loc['std']
    mads = aggs.loc['mad']
    xPeak = get_peak(rollingWindow, 'x')
    yPeak = get_peak(rollingWindow, 'y')
    zPeak = get_peak(rollingWindow, 'z')
    aggregate_df = pd.concat(
        [aggregate_df,
         pd.DataFrame({
             'user': user,
             'activity': activity,
             'XAVG': mean['x'],
             'YAVG': mean['y'],
             'ZAVG': mean['z'],
             'XSTANDDEV': stds['x'],
             'YSTANDDEV': stds['y'],
             'ZSTANDDEV': stds['y'],
             'XABSOLVDEV': mads['x'],
             'YABSOLVDEV': mads['y'],
             'ZABSOLVDEV': mads['z'],
             'XPEAK': xPeak,
             'YPEAK': yPeak,
             'ZPEAK': zPeak
         }, index=[0])],
        sort=False
    )

X_train, X_test, y_train, y_test = train_test_split(
    aggregate_df.drop(columns=['user', 'activity']),
    aggregate_df['activity'],
    test_size=0.3,
    random_state=1)

classifier = tree.DecisionTreeClassifier()
classifier = classifier.fit(X_train, y_train)

y_prediction = classifier.predict(X_test)
print('Accuracy:', metrics.accuracy_score(y_test, y_prediction))

code = m2c.export_to_java(classifier,
                          'at.awenzelhuemer.model',
                          'ActivityClassifier',
                          function_name='predictActivity')
with open('ActivityClassifier/src/at/awenzelhuemer/model/ActivityClassifier.java', 'w') as f:
    f.write(code)

aggregate_df.to_csv('ActivityClassifier/WISDM_ar_v1.1_transformed.csv')
