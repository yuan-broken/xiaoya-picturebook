package com.picturebook.book.controller;

import com.picturebook.book.domain.Book;
import com.picturebook.book.domain.BookPage;
import com.picturebook.book.domain.InteractionPoint;
import com.picturebook.book.service.BookService;
import com.picturebook.common.core.PageResult;
import com.picturebook.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端 - 绘本内容 API
 */
@Tag(name = "用户端-绘本内容", description = "绘本浏览、详情、页面、互动点")
@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "绘本列表（分页，支持搜索/年龄/主题筛选）")
    @GetMapping("/list")
    public Result<PageResult<Book>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) String theme) {
        return Result.ok(bookService.listBooks(pageNum, pageSize, keyword, ageGroup, theme));
    }

    @Operation(summary = "绘本详情")
    @GetMapping("/detail/{bookId}")
    public Result<Map<String, Object>> detail(@PathVariable Long bookId) {
        Book book = bookService.getBookDetail(bookId);
        List<BookPage> pages = bookService.getBookPages(bookId);
        Map<String, Object> data = new HashMap<>();
        data.put("book", book);
        data.put("pages", pages);
        return Result.ok(data);
    }

    @Operation(summary = "绘本页面列表")
    @GetMapping("/{bookId}/pages")
    public Result<List<BookPage>> pages(@PathVariable Long bookId) {
        return Result.ok(bookService.getBookPages(bookId));
    }

    @Operation(summary = "页面互动点列表")
    @GetMapping("/page/{pageId}/interactions")
    public Result<List<InteractionPoint>> pageInteractions(@PathVariable Long pageId) {
        return Result.ok(bookService.getPageInteractionPoints(pageId));
    }

    @Operation(summary = "绘本互动点列表")
    @GetMapping("/{bookId}/interactions")
    public Result<List<InteractionPoint>> bookInteractions(@PathVariable Long bookId) {
        return Result.ok(bookService.getBookInteractionPoints(bookId));
    }
}
