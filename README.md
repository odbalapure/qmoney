# QMoney

**QMoney**  is a portfolio analyzer that uses third party APIs like Tiingo and AlphaVantage to get the list of stocks and
calculate the annualized returns using the open/close price of the stocks.
- Applied ***design patterns*** to make the code modular and extensible so that more than one stock service(s) can be added
and also to increase the availability of the app if one of the services goes down.
- Enhanced the performance of the app by 23.5% by using multi-threading.
- Technologies Used: ***Spring RestTemplate*** for making REST API calls and fetch list of stocks, ***Jackson*** for serialization and
***JUnit*** for unit testing
