package pl.dworski.grpcwithspring.domain.model.paging

import spock.lang.Specification

class PageQuerySpec extends Specification {

    def "uses defaults when parameters are missing"() {
        expect:
        PageQuery.of(page, size) == expected

        where:
        page | size || expected
        null | null || new PageQuery(0, 20)
        3    | null || new PageQuery(3, 20)
        null | 10   || new PageQuery(0, 10)
    }

    def "caps size at the maximum"() {
        expect:
        new PageQuery(0, 51).size() == 50
        new PageQuery(0, 50).size() == 50
    }

    def "rejects invalid parameters"() {
        when:
        new PageQuery(page, size)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == message

        where:
        page | size || message
        -1   | 20   || 'page must not be negative'
        0    | 0    || 'size must be positive'
        0    | -5   || 'size must be positive'
    }

}
