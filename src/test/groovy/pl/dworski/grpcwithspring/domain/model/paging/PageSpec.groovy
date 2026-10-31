package pl.dworski.grpcwithspring.domain.model.paging

import spock.lang.Specification

class PageSpec extends Specification {

    def "computes total pages"() {
        expect:
        new Page([], 0, size, totalElements).totalPages() == expected

        where:
        totalElements | size || expected
        0             | 20   || 0
        1             | 20   || 1
        20            | 20   || 1
        21            | 20   || 2
        100           | 50   || 2
    }

}
