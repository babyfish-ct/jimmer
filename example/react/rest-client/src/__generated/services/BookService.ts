import type { Book } from '../model/entities';
import type { Unit, Page, BookInput, CompositeBookInput } from '../model/static';
import type { BookDto } from '../model/dto';
import type { Executor, Dynamic } from '../';

export class BookService {
    
    constructor(private executor: Executor) {}
    
    async deleteBook(options: BookServiceOptions['deleteBook']): Promise<
        Unit
    > {
        let uri = '/book/';
        uri += encodeURIComponent(options.id);
        return (await this.executor({uri, method: 'DELETE'})) as Unit
    }
    
    async findBooks(options: BookServiceOptions['findBooks']): Promise<
        Page<BookDto['BookService/DEFAULT_FETCHER']>
    > {
        let uri = '/book/list';
        uri += '?pageIndex=';
        uri += encodeURIComponent(options.pageIndex);
        uri += '&pageSize=';
        uri += encodeURIComponent(options.pageSize);
        uri += '&sortCode=';
        uri += encodeURIComponent(options.sortCode);
        uri += '&name=';
        uri += encodeURIComponent(options.name);
        if (options.storeName !== undefined && options.storeName !== null) {
            uri += '&storeName=';
            uri += encodeURIComponent(options.storeName);
        }
        if (options.authorName !== undefined && options.authorName !== null) {
            uri += '&authorName=';
            uri += encodeURIComponent(options.authorName);
        }
        return (await this.executor({uri, method: 'GET'})) as Page<BookDto['BookService/DEFAULT_FETCHER']>
    }
    
    async findComplexBooks(options: BookServiceOptions['findComplexBooks']): Promise<
        Page<BookDto['BookService/COMPLEX_FETCHER']>
    > {
        let uri = '/book/complexList';
        uri += '?pageIndex=';
        uri += encodeURIComponent(options.pageIndex);
        uri += '&pageSize=';
        uri += encodeURIComponent(options.pageSize);
        uri += '&sortCode=';
        uri += encodeURIComponent(options.sortCode);
        uri += '&name=';
        uri += encodeURIComponent(options.name);
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
    
    async findSimpleBooks(): Promise<
        ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>
    > {
        let uri = '/book/simpleList';
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>
    }
    
    async saveBook(options: BookServiceOptions['saveBook']): Promise<
        Dynamic<Book>
    > {
        let uri = '/book/';
        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<Book>
    }
    
    async saveBook_2(options: BookServiceOptions['saveBook_2']): Promise<
        Dynamic<Book>
    > {
        let uri = '/book/withChapters';
        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<Book>
    }
}

export type BookServiceOptions = {
    'deleteBook': {readonly id: number},
    'findBooks': {
        readonly pageIndex: number, 
        readonly pageSize: number, 
        readonly sortCode: string, 
        readonly name: string, 
        readonly storeName?: string, 
        readonly authorName?: string
    },
    'findComplexBooks': {
        readonly pageIndex: number, 
        readonly pageSize: number, 
        readonly sortCode: string, 
        readonly name: string, 
        readonly storeName?: string, 
        readonly authorName?: string
    },
    'findSimpleBooks': {},
    'saveBook': {readonly body: BookInput},
    'saveBook_2': {readonly body: CompositeBookInput}
}