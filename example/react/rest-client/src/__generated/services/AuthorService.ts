import type {Executor} from '../';
import type {AuthorDto} from '../model/dto/';
import type {Dynamic_Author} from '../model/dynamic/';
import type {AuthorInput, AuthorSpecification} from '../model/static/';

export class AuthorService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteAuthor: (options: AuthorServiceOptions['deleteAuthor']) => Promise<
        void
    > = async(options) => {
        let _uri = '/author/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findAuthors: (options: AuthorServiceOptions['findAuthors']) => Promise<
        ReadonlyArray<AuthorDto['AuthorService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/author/list';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.firstName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'firstName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.lastName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'lastName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.minCreatedTime;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'minCreatedTime='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.maxCreatedTime;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'maxCreatedTime='
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<AuthorDto['AuthorService/DEFAULT_FETCHER']>>;
    }
    
    readonly findComplexAuthor: (options: AuthorServiceOptions['findComplexAuthor']) => Promise<
        AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/author/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined>;
    }
    
    readonly findSimpleAuthors: () => Promise<
        ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    > = async() => {
        let _uri = '/author/simpleList';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>>;
    }
    
    readonly saveAuthor: (options: AuthorServiceOptions['saveAuthor']) => Promise<
        Dynamic_Author
    > = async(options) => {
        let _uri = '/author/';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<Dynamic_Author>;
    }
}

export type AuthorServiceOptions = {
    'findSimpleAuthors': {}, 
    'findAuthors': {
        readonly specification: AuthorSpecification, 
        readonly sortCode?: string | undefined
    }, 
    'findComplexAuthor': {
        readonly id: number
    }, 
    'saveAuthor': {
        readonly body: AuthorInput
    }, 
    'deleteAuthor': {
        readonly id: number
    }
}
