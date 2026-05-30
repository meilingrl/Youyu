package com.youyu.backend.filter;

import com.youyu.backend.common.support.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void putsProvidedTraceIdInMdcAndClearsItAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestContext.TRACE_ID_HEADER, "provided-trace-id");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertEquals("provided-trace-id", MDC.get(RequestContext.TRACE_ID_ATTRIBUTE)));

        assertEquals("provided-trace-id", request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE));
        assertEquals("provided-trace-id", response.getHeader(RequestContext.TRACE_ID_HEADER));
        assertNull(MDC.get(RequestContext.TRACE_ID_ATTRIBUTE));
    }

    @Test
    void clearsMdcWhenDownstreamThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                assertFalse(MDC.get(RequestContext.TRACE_ID_ATTRIBUTE).isBlank());
                throw new IllegalStateException("downstream failure");
            }));

        assertEquals("downstream failure", exception.getMessage());
        assertNull(MDC.get(RequestContext.TRACE_ID_ATTRIBUTE));
    }
}
