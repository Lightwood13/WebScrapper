## WebScrapper

An XPath web scrapper with control via REST API. Used technologies:

- Kotlin and Spring
- Postgres as DB, and H2 for testing
- Spring WebClient for retrieving web pages
- [Jsoup](https://github.com/jhy/jsoup) and [Xsoup](https://github.com/code4craft/xsoup) for HTML parsing

### Database set up

Build dockerfile with
```
docker docker build -t scrapper_db .
```
and run with
```
docker run --name scrapper_db -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d scrapper_db
```

### API endpoints
`GET /tasks`

```http request
POST /tasks

{
    "url": "https://github.com",
    "xpath": "//div",
    "enabled": true,
    "intervalMillis": 60000
}
```

```http request
PATCH /tasks?task_id=3

{
    "url": "https://google.com",
    "xpath": "//div/text()",
    "enabled": false,
    "intervalMillis": 120000
}
```

`DELETE /tasks?task_id=3`

`GET /parse-results?page=1&page-size=100`
