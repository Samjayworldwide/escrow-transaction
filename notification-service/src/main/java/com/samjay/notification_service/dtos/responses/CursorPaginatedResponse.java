package com.samjay.notification_service.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorPaginatedResponse<T> {

    private List<T> items;

    private String nextCursor;

    private boolean hasMore;

    private int pageSize;

}
