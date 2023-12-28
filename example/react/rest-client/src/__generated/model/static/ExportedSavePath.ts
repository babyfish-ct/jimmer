import type {ExportedSavePath_Node} from './';

export interface ExportedSavePath {
    readonly rootTypeName: string;
    readonly nodes: ReadonlyArray<ExportedSavePath_Node>;
}
