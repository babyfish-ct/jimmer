import type { Dynamic, Executor } from '../';
import type { BookDto } from '../model/dto';
import type { Book } from '../model/entities';
import type { BookInput, CompositeBookInput, Page } from '../model/static';

export class BookService {
    
    constructor(private executor: Executor) {}
    
    async deleteBook(options: BookServiceOptions['deleteBook']): Promise<void> {
        let _uri = '/book/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as void
    }
    
    async findBooks(options: BookServiceOptions['findBooks']): Promise<
        Page<BookDto['BookService/DEFAULT_FETCHER']>
    > {
        let _uri = '/book/list';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.pageIndex;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageIndex='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.pageSize;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageSize='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.sortCode;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'sortCode='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.name;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'name='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.storeName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'storeName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.authorName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'authorName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Page<BookDto['BookService/DEFAULT_FETCHER']>
    }
    
    async findComplexBook(options: BookServiceOptions['findComplexBook']): Promise<
        BookDto['BookService/COMPLEX_FETCHER'] | undefined
    > {
        let _uri = '/book/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as BookDto['BookService/COMPLEX_FETCHER'] | undefined
    }
    
    async findSimpleBooks(): Promise<
        ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>
    > {
        let _uri = '/book/simpleList';
        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>
    }
    
    async saveBook(options: BookServiceOptions['saveBook']): Promise<
        Dynamic<Book>
    > {
        let _uri = '/book/';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Dynamic<Book>
    }
    
    async saveCompositeBook(options: BookServiceOptions['saveCompositeBook']): Promise<
        Dynamic<Book>
    > {
        let _uri = '/book/withChapters';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Dynamic<Book>
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