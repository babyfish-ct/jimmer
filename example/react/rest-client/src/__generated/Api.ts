import type { Executor } from './';

import { AuthorService, BookService, BookStoreService, TreeService } from './services';

export class Api {
    
    readonly authorService: AuthorService;
    
    readonly bookService: BookService;
    
    readonly bookStoreService: BookStoreService;
    
    readonly treeService: TreeService;
    
    constructor(executor: Executor) {
        this.authorService = new AuthorService(executor);
        this.bookService = new BookService(executor);
        this.bookStoreService = new BookStoreService(executor);
        this.treeService = new TreeService(executor);
    }
}