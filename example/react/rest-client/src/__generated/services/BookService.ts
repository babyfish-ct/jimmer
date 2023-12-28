import type {Executor} from '../';
import type {BookDto} from '../model/dto/';
import type {Dynamic_Book} from '../model/dynamic/';
import type {
    BookInput, 
    BookSpecification, 
    CompositeBookInput, 
    Page
} from '../model/static/';

export class BookService {
    
    constructor(private executor: Executor) {}
    
    async deleteBook(options: BookServiceOptions['deleteBook']): Promise<void> {
        let _uri = '/book/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'}))
    }
    
    /**
     * The functionality of this method is the same as
     * {@link #findBooksBySuperQBE(int, int, String, BookSpecification)}
     */
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
        _value = options.minPrice;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'minPrice='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.maxPrice;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'maxPrice='
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
    
    /**
     * The functionality of this method is the same as
     * {@link #findBooks(int, int, String, String, BigDecimal, BigDecimal, String, String)}
     */
    async findBooksBySuperQBE(options: BookServiceOptions['findBooksBySuperQBE']): Promise<
        Page<BookDto['BookService/DEFAULT_FETCHER']>
    > {
        let _uri = '/book/list/bySuperQBE';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.name;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'name='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.minPrice;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'minPrice='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.maxPrice;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'maxPrice='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.storeName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'storeName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.authorName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'authorName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Page<BookDto['BookService/DEFAULT_FETCHER']>
    }
    
    async findComplexBook(options: BookServiceOptions['findComplexBook']): Promise<
        BookDto['BookService/COMPLEX_FETCHER'] | null | undefined
    > {
        let _uri = '/book/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as BookDto['BookService/COMPLEX_FETCHER'] | null | undefined
    }
    
    async findSimpleBooks(): Promise<
        ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>
    > {
        let _uri = '/book/simpleList';
        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>
    }
    
    async saveBook(options: BookServiceOptions['saveBook']): Promise<
        Dynamic_Book
    > {
        let _uri = '/book/';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.input})) as Dynamic_Book
    }
    
    async saveCompositeBook(options: BookServiceOptions['saveCompositeBook']): Promise<
        Dynamic_Book
    > {
        let _uri = '/book/composite';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.input})) as Dynamic_Book
    }
}
export type BookServiceOptions = {
    'findSimpleBooks': {}, 
    'findBooks': {
        readonly pageIndex?: number | null | undefined, 
        readonly pageSize?: number | null | undefined, 
        readonly sortCode?: string | null | undefined, 
        readonly name?: string | null | undefined, 
        readonly minPrice?: number | null | undefined, 
        readonly maxPrice?: number | null | undefined, 
        readonly storeName?: string | null | undefined, 
        readonly authorName?: string | null | undefined
    }, 
    'findBooksBySuperQBE': {
        readonly pageIndex?: number | null | undefined, 
        readonly pageSize?: number | null | undefined, 
        readonly sortCode?: string | null | undefined, 
        readonly specification: BookSpecification
    }, 
    'findComplexBook': {
        readonly id: number
    }, 
    'saveBook': {
        input: BookInput
    }, 
    'saveCompositeBook': {
        readonly input: CompositeBookInput
    }, 
    'deleteBook': {
        readonly id: number
    }
}
