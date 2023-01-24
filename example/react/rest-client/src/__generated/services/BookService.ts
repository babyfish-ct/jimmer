import type { Book } from '../model/entities';
import type { Page, BookInput, CompositeBookInput } from '../model/static';
import type { BookDto } from '../model/dto';
import type { Executor, Dynamic } from '../';

export class BookService {
    
    constructor(private executor: Executor) {}
    
    async findComplexBooks(options: BookServiceOptions['findComplexBooks']): Promise<
        Page<BookDto['BookService/COMPLEX_FETCHER']>
    > {
        let uri = '/books/complex';
        uri += '?pageIndex=';
        uri += encodeURIComponent(options.pageIndex);
        uri += '&pageSize=';
        uri += encodeURIComponent(options.pageSize);
        if (options.name !== undefined && options.name !== null) {
            uri += '&name=';
            uri += encodeURIComponent(options.name);
        }
        if (options.storeName !== undefined && options.storeName !== null) {
            uri += '&storeName=';
            uri += encodeURIComponent(options.storeName);
        }
        if (options.authorName !== undefined && options.authorName !== null) {
            uri += '&authorName=';
            uri += encodeURIComponent(options.authorName);
        }
        return (await this.executor({uri, method: 'GET'})) as Page<BookDto['BookService/COMPLEX_FETCHER']>
    }
    
    async findSimpleBooks(options: BookServiceOptions['findSimpleBooks']): Promise<
        Page<BookDto['BookService/SIMPLE_FETCHER']>
    > {
        let uri = '/books/simple';
        uri += '?pageIndex=';
        uri += encodeURIComponent(options.pageIndex);
        uri += '&pageSize=';
        uri += encodeURIComponent(options.pageSize);
        if (options.name !== undefined && options.name !== null) {
            uri += '&name=';
            uri += encodeURIComponent(options.name);
        }
        if (options.storeName !== undefined && options.storeName !== null) {
            uri += '&storeName=';
            uri += encodeURIComponent(options.storeName);
        }
        if (options.authorName !== undefined && options.authorName !== null) {
            uri += '&authorName=';
            uri += encodeURIComponent(options.authorName);
        }
        return (await this.executor({uri, method: 'GET'})) as Page<BookDto['BookService/SIMPLE_FETCHER']>
    }
    
    async saveBook(options: BookServiceOptions['saveBook']): Promise<
        Dynamic<Book>
    > {
        let uri = '/book/dynamic';
        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<Book>
    }
    
    async saveBook_2(options: BookServiceOptions['saveBook_2']): Promise<
        Dynamic<Book>
    > {
        let uri = '/book';
        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<Book>
    }
    
    async saveBook_3(options: BookServiceOptions['saveBook_3']): Promise<
        Dynamic<Book>
    > {
        let uri = '/book/withChapters';
        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<Book>
    }
}

export type BookServiceOptions = {
    'findComplexBooks': {
        readonly pageIndex: number, 
        readonly pageSize: number, 
        readonly name?: string, 
        readonly storeName?: string, 
        readonly authorName?: string
    },
    'findSimpleBooks': {
        readonly pageIndex: number, 
        readonly pageSize: number, 
        readonly name?: string, 
        readonly storeName?: string, 
        readonly authorName?: string
    },
    'saveBook': {readonly body: Dynamic<Book>},
    'saveBook_2': {readonly body: BookInput},
    'saveBook_3': {readonly body: CompositeBookInput}
}