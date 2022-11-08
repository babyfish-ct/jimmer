import React, { useCallback, useState } from "react";
import {
	G2,
	Chart,
	Tooltip,
	Interval,
	Interaction,
    Axis,
    Legend
} from "bizcharts";

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import FormGroup from '@mui/material/FormGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import Divider from '@mui/material/Divider';
import { ScaleOption } from "bizcharts/lib/interface";

export const Benchmark: React.FC<{
    readonly type: "OPS" | "TIME",
    readonly locale?: "zh"
}> = ({type, locale}) => {

    const [showJdbc, setShowJdbc] = useState(type !== "OPS");
    const onShowJdbcChange = useCallback((event: React.ChangeEvent<HTMLInputElement>, checked: boolean) => {
        setShowJdbc(checked);
    }, []);
    return (
        <Tabs>
            <TabItem value="chart" label={locale === "zh" ? "图表" : "Chart"} default>
                { 
                    type === "OPS" && 
                    <>
                        <FormGroup>
                            <FormControlLabel 
                            control={<Checkbox value={showJdbc} onChange={onShowJdbcChange}/>} 
                            label={locale === "zh" ? "显示原生JDBC指标" : "Show native JDBC metrics"}/>
                        </FormGroup> 
                        <Divider/>
                    </>
                }
                {
                    type == "OPS" ?
                    <BenchmarkChart rows={opsRows} scale={opsScale} showJdbc={showJdbc}/> :
                    <BenchmarkChart rows={timeRows} scale={timeScale} showJdbc={showJdbc}/>
                }
            </TabItem>
            <TabItem value="data" label={locale === "zh" ? "数据" : "Data"}>
                {
                    type === "OPS" ?
                    <BenchmarkTable rows={opsRows} valueTitle="Ops/s"/> :
                    <BenchmarkTable rows={timeRows} valueTitle="Time(μs)"/>
                }
            </TabItem>
        </Tabs>
    );
};

const BenchmarkChart: React.FC<{
    readonly rows: ReadonlyArray<Row>,
    readonly scale: {
        readonly [field: string]: ScaleOption
    },
    readonly showJdbc: boolean
}> = ({rows, scale, showJdbc}) => {
    return (
        <Chart 
        filter={showJdbc ? undefined : {"framework": (v: any) => !(v as string).startsWith("JDBC")}}
        height={400} 
        padding="auto" 
        data={rows} 
        scale={scale} 
        autoFit>
			<Interval
				adjust={[
					{
						type: 'dodge',
						marginRatio: 0,
					},
				]}
				color="framework"
				position="dataCount*value"
			/>
            <Axis name="value" title/>
            <Legend position="left"/>
			<Tooltip shared />
			<Interaction type="active-region" />
		</Chart>
    );
}

const BenchmarkTable: React.FC<{readonly rows: ReadonlyArray<Row>, readonly valueTitle: string}> = ({rows, valueTitle}) => {
    return (
        <Table>
            <TableHead>
                <TableRow>
                    <TableCell>Framework</TableCell>
                    <TableCell>Data count</TableCell>
                    <TableCell>{valueTitle}</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {
                    rows.map(row => 
                        <TableRow key={`${row.framework}-${row.dataCount}`}>
                            <TableCell>{row.framework}</TableCell>
                            <TableCell>{row.dataCount}</TableCell>
                            <TableCell>{row.value}</TableCell>
                        </TableRow>
                    )
                }
            </TableBody>
        </Table>
    );
};

const opsScale = {
	dataCount: {
		alias: "Data count"
	},
	value: {
		alias: "Ops/s"
	}
};

const timeScale = {
	dataCount: {
		alias: "Data count"
	},
	value: {
		alias: 'Time(μs)'
	}
};

interface Row {
    readonly framework: string;
    readonly dataCount: string;
    readonly value: number;
};

const rows: ReadonlyArray<Row> = [
    {framework: "JDBC(ColIndex)", dataCount: "10", value: 564001.2567729354},
    {framework: "JDBC(ColIndex)", dataCount: "20", value: 480692.0049806773},
    {framework: "JDBC(ColIndex)", dataCount: "50", value: 232400.81448051397},
    {framework: "JDBC(ColIndex)", dataCount: "100", value: 134662.65691855497},
    {framework: "JDBC(ColIndex)", dataCount: "200", value: 71072.13756007861},
    {framework: "JDBC(ColIndex)", dataCount: "500", value: 30750.505197085316},
    {framework: "JDBC(ColIndex)", dataCount: "1000", value: 14681.13949962451},
    {framework: "JDBC(ColName)", dataCount: "10", value: 340040.8951522477},
    {framework: "JDBC(ColName)", dataCount: "20", value: 194773.9145513743},
    {framework: "JDBC(ColName)", dataCount: "50", value: 125966.97513243598},
    {framework: "JDBC(ColName)", dataCount: "100", value: 55367.65334694574},
    {framework: "JDBC(ColName)", dataCount: "200", value: 38822.98374903588},
    {framework: "JDBC(ColName)", dataCount: "500", value: 16254.22473511859},
    {framework: "JDBC(ColName)", dataCount: "1000", value: 8157.27625556836},
    {framework: "Jimmer(Java)", dataCount: "10", value: 234768.53510339226},
    {framework: "Jimmer(Java)", dataCount: "20", value: 135347.1018453932},
    {framework: "Jimmer(Java)", dataCount: "50", value: 70747.2920672707},
    {framework: "Jimmer(Java)", dataCount: "100", value: 35901.15112642172},
    {framework: "Jimmer(Java)", dataCount: "200", value: 18584.529478668042},
    {framework: "Jimmer(Java)", dataCount: "500", value: 7660.571335425491},
    {framework: "Jimmer(Java)", dataCount: "1000", value: 3758.5644619620043},
    {framework: "Jimmer(Kotlin)", dataCount: "10", value: 231853.04285935243},
    {framework: "Jimmer(Kotlin)", dataCount: "20", value: 133593.8293837718},
    {framework: "Jimmer(Kotlin)", dataCount: "50", value: 61120.85011656247},
    {framework: "Jimmer(Kotlin)", dataCount: "100", value: 27804.709414444893},
    {framework: "Jimmer(Kotlin)", dataCount: "200", value: 16830.479649806184},
    {framework: "Jimmer(Kotlin)", dataCount: "500", value: 6919.78158024675},
    {framework: "Jimmer(Kotlin)", dataCount: "1000", value: 3485.590552457007},
    {framework: "MyBatis", dataCount: "10", value: 77973.45980995704},
    {framework: "MyBatis", dataCount: "20", value: 43832.823890403015},
    {framework: "MyBatis", dataCount: "50", value: 20452.94226988173},
    {framework: "MyBatis", dataCount: "100", value: 10489.448602735492},
    {framework: "MyBatis", dataCount: "200", value: 5146.512081854517},
    {framework: "MyBatis", dataCount: "500", value: 1921.8943211405174},
    {framework: "MyBatis", dataCount: "1000", value: 1059.434126831536},
    {framework: "Exposed", dataCount: "10", value: 96616.78513212189},
    {framework: "Exposed", dataCount: "20", value: 69563.16749094054},
    {framework: "Exposed", dataCount: "50", value: 34146.01603428918},
    {framework: "Exposed", dataCount: "100", value: 19736.116951748565},
    {framework: "Exposed", dataCount: "200", value: 10248.907897197449},
    {framework: "Exposed", dataCount: "500", value: 4261.024374795272},
    {framework: "Exposed", dataCount: "1000", value: 2182.4893364493373},
    {framework: "JPA(Hibernate)", dataCount: "10", value: 103156.13364887898},
    {framework: "JPA(Hibernate)", dataCount: "20", value: 60657.86283377519},
    {framework: "JPA(Hibernate)", dataCount: "50", value: 26729.97622160438},
    {framework: "JPA(Hibernate)", dataCount: "100", value: 13504.199265684878},
    {framework: "JPA(Hibernate)", dataCount: "200", value: 6850.270944007042},
    {framework: "JPA(Hibernate)", dataCount: "500", value: 2540.475589159096},
    {framework: "JPA(Hibernate)", dataCount: "1000", value: 1436.778709930832},
    {framework: "JPA(EclipseLink)", dataCount: "10", value: 63771.749254104725},
    {framework: "JPA(EclipseLink)", dataCount: "20", value: 33636.783791773945},
    {framework: "JPA(EclipseLink)", dataCount: "50", value: 13524.821332500553},
    {framework: "JPA(EclipseLink)", dataCount: "100", value: 6815.290869847297},
    {framework: "JPA(EclipseLink)", dataCount: "200", value: 3232.206328003842},
    {framework: "JPA(EclipseLink)", dataCount: "500", value: 1361.0075376064835},
    {framework: "JPA(EclipseLink)", dataCount: "1000", value: 666.3806417880297},
    {framework: "JOOQ", dataCount: "10", value: 57409.591343825436},
    {framework: "JOOQ", dataCount: "20", value: 35125.83069317591},
    {framework: "JOOQ", dataCount: "50", value: 14441.417555740112},
    {framework: "JOOQ", dataCount: "100", value: 9954.880100551949},
    {framework: "JOOQ", dataCount: "200", value: 3738.1415218764314},
    {framework: "JOOQ", dataCount: "500", value: 1689.4977546444927},
    {framework: "JOOQ", dataCount: "1000", value: 994.240300985835},
    {framework: "Nutz", dataCount: "10", value: 82880.5218855578},
    {framework: "Nutz", dataCount: "20", value: 48007.869458932815},
    {framework: "Nutz", dataCount: "50", value: 17310.36987144335},
    {framework: "Nutz", dataCount: "100", value: 8570.213250226707},
    {framework: "Nutz", dataCount: "200", value: 5123.799062062122},
    {framework: "Nutz", dataCount: "500", value: 1891.545739299192},
    {framework: "Nutz", dataCount: "1000", value: 963.5871325666997},
    {framework: "ObjectiveSQL", dataCount: "10", value: 59701.04409818739},
    {framework: "ObjectiveSQL", dataCount: "20", value: 29487.308838276065},
    {framework: "ObjectiveSQL", dataCount: "50", value: 12662.44231891958},
    {framework: "ObjectiveSQL", dataCount: "100", value: 6795.016060183564},
    {framework: "ObjectiveSQL", dataCount: "200", value: 3419.3520576008223},
    {framework: "ObjectiveSQL", dataCount: "500", value: 1355.7026828586477},
    {framework: "ObjectiveSQL", dataCount: "1000", value: 598.5931780214165},
    {framework: "Spring Data JDBC", dataCount: "10", value: 20612.491148409532},
    {framework: "Spring Data JDBC", dataCount: "20", value: 10123.095370535204},
    {framework: "Spring Data JDBC", dataCount: "50", value: 4029.2231032104232},
    {framework: "Spring Data JDBC", dataCount: "100", value: 1984.4450348794267},
    {framework: "Spring Data JDBC", dataCount: "200", value: 1078.6076478516723},
    {framework: "Spring Data JDBC", dataCount: "500", value: 411.6478839875141},
    {framework: "Spring Data JDBC", dataCount: "1000", value: 226.5922089775573},
    {framework: "Ktorm", dataCount: "10", value: 17717.284985056343},
    {framework: "Ktorm", dataCount: "20", value: 10213.518906877975},
    {framework: "Ktorm", dataCount: "50", value: 4181.627630394707},
    {framework: "Ktorm", dataCount: "100", value: 2040.6740629521064},
    {framework: "Ktorm", dataCount: "200", value: 1067.170484739404},
    {framework: "Ktorm", dataCount: "500", value: 356.0009982293281},
    {framework: "Ktorm", dataCount: "1000", value: 180.52960713759404}
];

const opsRows = rows.map(item => ({...item, value: Math.round(item.value)}));

const timeRows = rows.map(item => ({...item, value: Math.round(1000000 / item.value)}));
