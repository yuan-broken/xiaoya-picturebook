package com.picturebook.admin.controller;

import com.picturebook.book.domain.Book;
import com.picturebook.book.service.BookService;
import com.picturebook.common.core.PageResult;
import com.picturebook.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端：绘本内容管理
 * 对应 PRD M08 绘本内容管理。
 * 管理端接口前缀 /api/admin/book。
 */
@RestController
@RequestMapping("/api/admin/book")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;

    /**
     * 管理端：分页查询绘本列表
     */
    @GetMapping("/list")
    public Result<PageResult<Book>> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
        return Result.ok(bookService.adminList(pageNum, pageSize, keyword, status));
    }

    /**
     * 管理端：新增绘本
     */
    @PostMapping
    public Result<Long> add(@RequestBody Book book) {
        bookService.addBook(book);
        return Result.ok(book.getBookId());
    }

    /**
     * 管理端：修改绘本
     */
    @PutMapping
    public Result<Void> update(@RequestBody Book book) {
        bookService.updateBook(book);
        return Result.ok();
    }

    /**
     * 管理端：上架/下架
     */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable("id") Long bookId, @RequestParam("status") Integer status) {
        bookService.changeStatus(bookId, status);
        return Result.ok();
    }

    /**
     * 管理端：删除绘本（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long bookId) {
        bookService.removeById(bookId);
        return Result.ok();
    }
}
