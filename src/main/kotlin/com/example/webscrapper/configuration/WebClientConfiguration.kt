package com.example.webscrapper.configuration

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient


@Configuration
class WebClientConfiguration {

    @Value("\${web-scrapper.buffer-size}")
    var bufferSize: Int = 65536

    @Value("\${web-scrapper.connect-timeout-millis}")
    var connectTimeOutMillis: Int = 2000

    @Bean
    fun clientHttpConnector(): ClientHttpConnector =
        ReactorClientHttpConnector(
            HttpClient.create()
                .followRedirect(true)
                .headers { builder -> builder.set("Accept", "text/html") }
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOutMillis)
        )

    @Bean
    fun webClient(clientHttpConnector: ClientHttpConnector): WebClient =
        WebClient.builder()
            .clientConnector(clientHttpConnector)
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { codecs -> codecs.defaultCodecs().maxInMemorySize(bufferSize) }
                    .build()
            )
            .build()
}