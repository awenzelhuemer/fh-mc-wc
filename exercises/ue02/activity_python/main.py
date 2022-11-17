import pandas as pd
import m2cgen as m2c
from sklearn.model_selection import train_test_split
from sklearn import tree
from sklearn.model_selection import TimeSeriesSplit
from sklearn import preprocessing

df = pd.read_csv('WISDM_ar_v1.1_raw.txt')
df = df.iloc[:, 1:]
print(df.info())

number = preprocessing.LabelEncoder()

df['timestamp'] = pd.to_datetime(df['timestamp'])
X = df.iloc[1:]
y = number.fit_transform(df['activity'])

n_splits = 5
ts_cv = TimeSeriesSplit(gap=0, max_train_size=None, n_splits=n_splits, test_size=None)
splitted = ts_cv.split(X)

for train_index, test_index in splitted:
    print("TRAIN:", train_index, "TEST:", test_index)
    X_train, X_test = X.iloc[train_index[0]:train_index[-1]], X.iloc[test_index[0]:test_index[-1]]
    y_train, y_test = y.iloc[train_index[0]:train_index[-1]], y.iloc[test_index[0]:test_index[-1]]
    # print((X_train['timestamp'].max() - X_train['timestamp'].min()))
    classifier = tree.DecisionTreeClassifier()
    classifier.fit(X_train, y_train)
    print(classifier.predict(X_test))

# code = m2c.export_to_c(classifier)

# f = open('test.c', 'w')
# f.write(code)
# f.close()

