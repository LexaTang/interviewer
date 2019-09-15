import { Layout, Typography, Row, Col, Card, Form, Slider, Input, Button } from 'antd';

import styles from './room.less';
import { connect } from 'dva';
import { Component } from 'react';

const { Header } = Layout;
const { Title } = Typography;
const { TextArea } = Input;

class CommentForm extends Component {
  submitHandler = (e) => {
    const { form, resultHandler } = this.props;

    e.preventDefault();
    form.validateFields((err, values) => {
      if (!err) {
        resultHandler({ payloads: values, room: this.props.room });
        form.resetFields();
      }
    });
  }

  render() {
    const { form } = this.props;

    return (
      <Form onSubmit={this.submitHandler}>
        <Form.Item label="评分1">
          {form.getFieldDecorator('grade1', {
            rules: [{ required: true, message: '请输入分数！' }],
          })(<Slider max={10} marks={{ 0: '0', 6: '6', 10: '10' }} />)}
        </Form.Item>
        <Form.Item label="评分2">
          {form.getFieldDecorator('grade2', {
            rules: [{ required: true, message: '请输入分数！' }],
          })(<Slider max={10} marks={{ 0: '0', 6: '6', 10: '10' }} />)}
        </Form.Item>
        <Form.Item label="评分3">
          {form.getFieldDecorator('grade3', {
            rules: [{ required: true, message: '请输入分数！' }],
          })(<Slider max={10} marks={{ 0: '0', 6: '6', 10: '10' }} />)}
        </Form.Item>
        <Form.Item label="评论">
          {form.getFieldDecorator('comment')(<TextArea rows="4" />)}
        </Form.Item>
        <Button type="primary" htmlType="submit" >
          提交
        </Button>
      </Form>
    );
  }
}
const CommentComponent = Form.create()(CommentForm);

@connect(({ loading, room }) => ({
  loading,
  interviewing: room.interviewing,
  next: room.next,
  info: room.info,
}))
class Room extends Component {
  componentDidMount() {
    const { room } = this.props.match.params;
    const { dispatch } = this.props;
    window.setInterval(() => dispatch({type: 'room/fetch', room }), 1000);
  }

  render() {
    const { room, interviewer } = this.props.match.params;
    const { interviewing, next, info, dispatch } = this.props;

    return (
      <div>
        <Header>
          <Title className="title">
            {interviewer}面试官{room}房间
          </Title>
        </Header>
        <br />
        <Row gutter={16}>
          <Col xs={0} sm={0} md={0} lg={1} xl={3} />
          <Col xs={24} sm={24} md={24} lg={22} xl={11}>
            <Card title={interviewing ? interviewing : '受试者'}>
              {info && Object.keys(info).length ? 'infoFormatter' : '个人信息'}
            </Card>
            <br />
            <Typography>下一位面试者：{next}</Typography>
            <Button onClick={() => dispatch({type: 'room/fetch', room })}>拉取</Button>
          </Col>
          <Col xs={1} sm={1} md={1} lg={2} xl={0} />
          <Col xs={22} sm={22} md={22} lg={20} xl={7} >
            <CommentComponent room={room} resultHandler={(_) => dispatch({type: 'room/comm', ..._})}/>
          </Col>
        </Row>
      </div>
    );
  }
}

export default Room;