import type { Dynamic, Executor } from '../';
import type { TreeNodeDto } from '../model/dto';
import type { TreeNode } from '../model/entities';
import type { RecursiveTreeInput } from '../model/static';

export class TreeService {
    
    constructor(private executor: Executor) {}
    
    async deleteTree(options: TreeServiceOptions['deleteTree']): Promise<void> {
        let _uri = '/tree/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as void
    }
    
    async findRootTrees(options?: TreeServiceOptions['findRootTrees']): Promise<
        ReadonlyArray<TreeNodeDto['TreeService/RECURSIVE_FETCHER']>
    > {
        let _uri = '/tree/roots/recursive';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options?.rootName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += '=rootName'
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<TreeNodeDto['TreeService/RECURSIVE_FETCHER']>
    }
    
    async saveTree(options: TreeServiceOptions['saveTree']): Promise<
        Dynamic<TreeNode>
    > {
        let _uri = '/tree/root/recursive';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Dynamic<TreeNode>
    }
}

export type TreeServiceOptions = {
    'deleteTree': {readonly id: number},
    'findRootTrees': {readonly rootName?: string},
    'saveTree': {readonly body: RecursiveTreeInput}
}