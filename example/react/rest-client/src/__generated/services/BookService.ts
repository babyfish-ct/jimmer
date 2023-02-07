import type { Dynamic, Executor } from '../';
import type { BookDto } from '../model/dto';
import type { Book } from '../model/entities';
import type { BookInput, CompositeBookInput, Page } from '../model/static';

export class BookService {
    
    constructor(private executor: Executor) {}
    
    async deleteBook(options: BookServiceOptions['deleteBook']): Promise<void> {
        let uri = '/book/';
        uri += encodeURIComponent(options.id);
        return (await this.executor({uri, method: 'DELETE'})) as void
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
        return (await this.executor({uri, method: 'GET'})) as Page<BookDto['BookService/DEFAULT_FETCHER']>
    }
    
    async findComplexBook(options: BookServiceOptions['findComplexBook']): Promise<
        BookDto['BookService/COMPLEX_FETCHER'] | undefined
    > {
        let uri = '/book/';
        uri += encodeURIComponent(options.id);
        return (await this.executor({uri, method: 'GET'})) as BookDto['BookService/COMPLEX_FETCHER'] | undefined
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
    
    async saveCompositeBook(options: BookServiceOptions['saveCompositeBook']): Promise<
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
        readonly name?: string, 
        readonly storeName?: string, 
        readonly authorName?: string
    },
    'findComplexBook': {readonly id: number},
    'findSimpleBooks': {},
    'saveBook': {readonly body: BookInput},
    'saveCompositeBook': {readonly body: CompositeBookInput}
}